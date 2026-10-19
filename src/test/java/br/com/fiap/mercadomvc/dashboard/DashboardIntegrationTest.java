package br.com.fiap.mercadomvc.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.fiap.mercadomvc.compra.dto.CompraCreateRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraItemRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentResult;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxRepository;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.compra.service.CompraService;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardView;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotificationRepository;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
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
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private EmailNotificationRepository notificationRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limparBanco() {
        notificationRepository.deleteAll();
        outboxRepository.deleteAll();
        compraRepository.deleteAll();
        mercadoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void somenteAdminAcessaDashboardEVisualizaEntradaNaNavegacao() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(content().string(containsString("Receita aprovada")));

        mockMvc.perform(get("/admin/dashboard").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/mercado").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("/admin/dashboard")));

        mockMvc.perform(get("/mercado").with(user("cliente").roles("CLIENTE")))
                .andExpect(content().string(not(containsString("/admin/dashboard"))));
    }

    @Test
    void bancoVazioRetornaTotaisZeradosEColecoesVazias() throws Exception {
        DashboardView dashboard = dashboardService.carregar();

        assertThat(dashboard.receitaTotal()).isEqualByComparingTo("0.00");
        assertThat(dashboard.lucroTotal()).isEqualByComparingTo("0.00");
        assertThat(dashboard.comprasAprovadas()).isZero();
        assertThat(dashboard.comprasPendentes()).isZero();
        assertThat(dashboard.comprasRejeitadas()).isZero();
        assertThat(dashboard.inventario()).isEmpty();
        assertThat(dashboard.compras()).isEmpty();
        assertThat(dashboard.graficoFinanceiro()).isEmpty();
        assertThat(dashboard.graficoEstoque()).isEmpty();

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sem vendas aprovadas")))
                .andExpect(content().string(containsString("Sem estoque para representar")))
                .andExpect(content().string(not(containsString("data-chart=\"financial\""))))
                .andExpect(content().string(not(containsString("data-chart=\"inventory\""))));
    }

    @Test
    void calculaFinanceiroPorSnapshotsEExcluiPendentesERejeitadas() throws Exception {
        Usuario cliente = usuarioRepository.save(Usuario.builder()
                .nome("Cliente Dashboard")
                .login("cliente.dashboard")
                .email("cliente.dashboard@example.test")
                .senha("nao-utilizada-no-teste")
                .ativo(true)
                .perfil(PerfilUsuario.CLIENTE)
                .build());
        Mercado produto = mercadoRepository.save(Mercado.builder()
                .nome("Cafe Dashboard")
                .tipo("Alimento")
                .setor("Mercearia")
                .tamanho("500 g")
                .preco(new BigDecimal("12.50"))
                .custoAquisicao(new BigDecimal("7.25"))
                .quantidadeEstoque(10)
                .build());

        CompraResponse aprovada = criarCompra(cliente, produto, 2);
        aplicarResultado(aprovada, PaymentStatus.APPROVED);
        CompraResponse pendente = criarCompra(cliente, produto, 1);
        CompraResponse rejeitada = criarCompra(cliente, produto, 3);
        aplicarResultado(rejeitada, PaymentStatus.REJECTED);

        Mercado produtoAtual = mercadoRepository.findById(produto.getId()).orElseThrow();
        produtoAtual.setPreco(new BigDecimal("999.99"));
        produtoAtual.setCustoAquisicao(new BigDecimal("998.99"));
        mercadoRepository.saveAndFlush(produtoAtual);

        DashboardView dashboard = dashboardService.carregar();

        assertThat(dashboard.receitaTotal()).isEqualByComparingTo("25.00");
        assertThat(dashboard.lucroTotal()).isEqualByComparingTo("10.50");
        assertThat(dashboard.comprasAprovadas()).isEqualTo(1);
        assertThat(dashboard.comprasPendentes()).isEqualTo(1);
        assertThat(dashboard.comprasRejeitadas()).isEqualTo(1);
        assertThat(dashboard.inventario()).singleElement()
                .satisfies(item -> assertThat(item.quantidadeEstoque()).isEqualTo(7));
        assertThat(dashboard.graficoFinanceiro())
                .extracting(barra -> barra.valor())
                .containsExactly(new BigDecimal("25.00"), new BigDecimal("10.50"));
        assertThat(dashboard.graficoEstoque()).singleElement()
                .satisfies(barra -> {
                    assertThat(barra.produtoId()).isEqualTo(produto.getId());
                    assertThat(barra.quantidade()).isEqualTo(7);
                    assertThat(barra.largura()).isPositive();
                });
        assertThat(dashboard.compras()).hasSize(3);
        assertThat(dashboard.compras())
                .extracting(compra -> compra.total())
                .containsExactlyInAnyOrder(
                        new BigDecimal("25.00"),
                        new BigDecimal("12.50"),
                        new BigDecimal("37.50"));
        assertThat(pendente.status().name()).isEqualTo("PENDING_PAYMENT");

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cafe Dashboard")))
                .andExpect(content().string(containsString("Cliente Dashboard")))
                .andExpect(content().string(containsString("R$ 25,00")))
                .andExpect(content().string(containsString("R$ 10,50")))
                .andExpect(content().string(containsString("data-chart=\"financial\"")))
                .andExpect(content().string(containsString("data-revenue=\"25.00\"")))
                .andExpect(content().string(containsString("data-profit=\"10.50\"")))
                .andExpect(content().string(containsString("data-chart=\"inventory\"")))
                .andExpect(content().string(containsString("data-stock=\"7\"")))
                .andExpect(content().string(containsString("Aprovada")))
                .andExpect(content().string(containsString("Pendente")))
                .andExpect(content().string(containsString("Rejeitada")));
    }

    private CompraResponse criarCompra(Usuario cliente, Mercado produto, int quantidade) {
        return compraService.criar(
                cliente.getLogin(),
                new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), quantidade))));
    }

    private void aplicarResultado(CompraResponse compra, PaymentStatus status) {
        compraService.processarResultadoPagamento(new PaymentResult(
                compra.id(),
                compra.correlationId(),
                compra.total(),
                compra.moeda(),
                status));
    }
}
