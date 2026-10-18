package br.com.fiap.mercadomvc.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"postgres", "test"})
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompraRepository compraRepository;

    @BeforeEach
    void limparUsuarios() {
        compraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveValidarDadosDoCadastro() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nome", "")
                        .param("login", "ab")
                        .param("senha", "curta")
                        .param("confirmacaoSenha", "diferente"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("usuario", "nome", "login", "senha", "senhaConfirmada"));

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void deveImpedirLoginDuplicadoIgnorandoMaiusculas() throws Exception {
        cadastrar("Cliente Um", "cliente.teste", "senha-segura");

        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nome", "Cliente Dois")
                        .param("login", "CLIENTE.TESTE")
                        .param("senha", "outra-senha")
                        .param("confirmacaoSenha", "outra-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("usuario", "login"));

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void devePersistirClienteComSenhaBcrypt() throws Exception {
        String senhaAberta = "senha-segura";

        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nome", "Cliente Teste")
                        .param("login", " Cliente.Teste ")
                        .param("senha", senhaAberta)
                        .param("confirmacaoSenha", senhaAberta))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        Usuario usuario = usuarioRepository.findByLoginIgnoreCase("cliente.teste").orElseThrow();
        assertThat(usuario.getNome()).isEqualTo("Cliente Teste");
        assertThat(usuario.getLogin()).isEqualTo("cliente.teste");
        assertThat(usuario.getSenha()).isNotEqualTo(senhaAberta);
        assertThat(passwordEncoder.matches(senhaAberta, usuario.getSenha())).isTrue();
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.CLIENTE);
    }

    private void cadastrar(String nome, String login, String senha) {
        usuarioRepository.save(Usuario.builder()
                .nome(nome)
                .login(login)
                .senha(passwordEncoder.encode(senha))
                .ativo(true)
                .perfil(PerfilUsuario.CLIENTE)
                .build());
    }
}
