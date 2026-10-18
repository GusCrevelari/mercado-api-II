package br.com.fiap.mercadomvc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"postgres", "test"})
class MercadoCrudCompatibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CompraRepository compraRepository;

    @BeforeEach
    void limparBanco() {
        compraRepository.deleteAll();
        mercadoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void devePreservarFluxoCompletoDoCrudMvcParaAdmin() throws Exception {
        var admin = user("admin").roles("ADMIN");

        mockMvc.perform(post("/mercado")
                        .with(admin)
                        .with(csrf())
                        .param("nome", "Cafe especial")
                        .param("tipo", "Bebida")
                        .param("setor", "Mercearia")
                        .param("tamanho", "500 g")
                        .param("preco", "19.90")
                        .param("quantidadeEstoque", "20")
                        .param("custoAquisicao", "11.75"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mercado"));

        Mercado criado = mercadoRepository.findAll().get(0);
        assertThat(criado.getQuantidadeEstoque()).isEqualTo(20);
        assertThat(criado.getCustoAquisicao()).isEqualByComparingTo(new BigDecimal("11.75"));
        mockMvc.perform(get("/mercado/{id}", criado.getId()).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cafe especial")));

        mockMvc.perform(put("/mercado/{id}", criado.getId())
                        .with(admin)
                        .with(csrf())
                        .param("nome", "Cafe atualizado")
                        .param("tipo", "Bebida")
                        .param("setor", "Mercearia")
                        .param("tamanho", "500 g")
                        .param("preco", "21.50")
                        .param("quantidadeEstoque", "35")
                        .param("custoAquisicao", "12.20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mercado/" + criado.getId()));

        Mercado atualizado = mercadoRepository.findById(criado.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Cafe atualizado");
        assertThat(atualizado.getPreco()).isEqualByComparingTo(new BigDecimal("21.50"));
        assertThat(atualizado.getQuantidadeEstoque()).isEqualTo(35);
        assertThat(atualizado.getCustoAquisicao()).isEqualByComparingTo(new BigDecimal("12.20"));

        mockMvc.perform(delete("/mercado/{id}", criado.getId()).with(admin).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mercado"));
        assertThat(mercadoRepository.existsById(criado.getId())).isFalse();
    }

    @Test
    void deveOcultarAcoesAdministrativasDoCliente() throws Exception {
        mercadoRepository.save(Mercado.builder()
                .nome("Arroz")
                .tipo("Alimento")
                .setor("Mercearia")
                .tamanho("1 kg")
                .preco(new BigDecimal("10.00"))
                .build());

        mockMvc.perform(get("/mercado").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Novo Produto"))))
                .andExpect(content().string(not(containsString(">Editar<"))))
                .andExpect(content().string(not(containsString(">Excluir<"))));
    }
}
