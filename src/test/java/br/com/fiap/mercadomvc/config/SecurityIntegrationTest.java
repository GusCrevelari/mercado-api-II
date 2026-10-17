package br.com.fiap.mercadomvc.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.mercadomvc.repository.MercadoRepository;
import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"postgres", "test"})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararBanco() {
        mercadoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAutenticarUsuarioPersistido() throws Exception {
        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Teste")
                .login("cliente")
                .senha(passwordEncoder.encode("senha-segura"))
                .ativo(true)
                .perfil(PerfilUsuario.CLIENTE)
                .build());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "CLIENTE")
                        .param("password", "senha-segura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mercado"))
                .andExpect(authenticated().withUsername("cliente").withRoles("CLIENTE"));
    }

    @Test
    void naoDeveAutenticarUsuarioInativo() throws Exception {
        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Inativo")
                .login("inativo")
                .senha(passwordEncoder.encode("senha-segura"))
                .ativo(false)
                .perfil(PerfilUsuario.CLIENTE)
                .build());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "inativo")
                        .param("password", "senha-segura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void deveRedirecionarAnonimoParaLogin() throws Exception {
        mockMvc.perform(get("/mercado"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void clientePodeConsultarMasRecebeForbiddenNasMutacoesMvc() throws Exception {
        var cliente = user("cliente").roles("CLIENTE");

        mockMvc.perform(get("/mercado").with(cliente))
                .andExpect(status().isOk());
        mockMvc.perform(get("/mercado/novo").with(cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/mercado").with(cliente).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/mercado/1").with(cliente).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/mercado/1").with(cliente).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientePodeConsultarMasRecebeForbiddenNasMutacoesRest() throws Exception {
        var cliente = user("cliente").roles("CLIENTE");

        mockMvc.perform(get("/api/mercado").with(cliente))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/mercado")
                        .with(cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/mercado/1").with(cliente).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/mercado/1").with(cliente).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/mercado/1").with(cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeAcessarFormularioECriarProduto() throws Exception {
        var admin = user("admin").roles("ADMIN");

        mockMvc.perform(get("/mercado/novo").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/mercado")
                        .with(admin)
                        .with(csrf())
                        .param("nome", "Cafe")
                        .param("tipo", "Bebida")
                        .param("setor", "Mercearia")
                        .param("tamanho", "500 g")
                        .param("preco", "19.90"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mercado"));

        mockMvc.perform(post("/api/mercado")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cha",
                                  "tipo": "Bebida",
                                  "setor": "Mercearia",
                                  "tamanho": "20 unidades",
                                  "preco": 12.50
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
