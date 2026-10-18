package br.com.fiap.mercadomvc.compra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.mercadomvc.compra.dto.CompraCreateRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraItemRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentProcessingOutcome;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentResult;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.compra.service.CompraService;
import br.com.fiap.mercadomvc.model.Mercado;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"postgres", "test"})
class CompraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompraService compraService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparBanco() {
        compraRepository.deleteAll();
        mercadoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveCriarCompraReservarEstoqueECalcularTotalNoServidor() throws Exception {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);

        mockMvc.perform(post("/api/compras")
                        .with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"produtoId": %d, "quantidade": 3}
                                  ]
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteLogin").value("cliente"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total").value(37.50))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(12.50))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3));

        assertThat(compraRepository.count()).isEqualTo(1);
        assertThat(mercadoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(7);
    }

    @Test
    void deveAceitarMultiplosProdutosESomarOsSubtotais() throws Exception {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado arroz = criarProduto("Arroz", "10.00", "6.00", 8);
        Mercado feijao = criarProduto("Feijao", "7.25", "4.00", 6);

        mockMvc.perform(post("/api/compras")
                        .with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"produtoId": %d, "quantidade": 2},
                                    {"produtoId": %d, "quantidade": 3}
                                  ]
                                }
                                """.formatted(arroz.getId(), feijao.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(41.75))
                .andExpect(jsonPath("$.itens.length()").value(2));

        assertThat(mercadoRepository.findById(arroz.getId()).orElseThrow().getQuantidadeEstoque()).isEqualTo(6);
        assertThat(mercadoRepository.findById(feijao.getId()).orElseThrow().getQuantidadeEstoque()).isEqualTo(3);
    }

    @Test
    void deveRejeitarQuantidadeInvalidaSemReservarEstoque() throws Exception {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);

        mockMvc.perform(post("/api/compras")
                        .with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": [{"produtoId": %d, "quantidade": 0}]}
                                """.formatted(produto.getId())))
                .andExpect(status().isBadRequest());

        assertThat(compraRepository.count()).isZero();
        assertThat(mercadoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(10);
    }

    @Test
    void deveImpedirCompraComEstoqueInsuficienteSemReservaParcial() throws Exception {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado disponivel = criarProduto("Arroz", "10.00", "6.00", 5);
        Mercado insuficiente = criarProduto("Feijao", "7.25", "4.00", 1);

        mockMvc.perform(post("/api/compras")
                        .with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"produtoId": %d, "quantidade": 2},
                                    {"produtoId": %d, "quantidade": 2}
                                  ]
                                }
                                """.formatted(disponivel.getId(), insuficiente.getId())))
                .andExpect(status().isConflict());

        assertThat(compraRepository.count()).isZero();
        assertThat(mercadoRepository.findById(disponivel.getId()).orElseThrow().getQuantidadeEstoque()).isEqualTo(5);
        assertThat(mercadoRepository.findById(insuficiente.getId()).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);
    }

    @Test
    void clienteVeSomenteAsPropriasComprasENaoAcessaCompraAlheia() throws Exception {
        criarUsuario("cliente.um", PerfilUsuario.CLIENTE);
        criarUsuario("cliente.dois", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);
        CompraResponse compraUm = compraService.criar(
                "cliente.um", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 1))));
        compraService.criar(
                "cliente.dois", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 1))));

        mockMvc.perform(get("/api/compras").with(user("cliente.um").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clienteLogin").value("cliente.um"));

        mockMvc.perform(get("/api/compras/{id}", compraUm.id())
                        .with(user("cliente.dois").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminVeTodasAsComprasMasNaoPodeCriarCompraComoCliente() throws Exception {
        criarUsuario("cliente.um", PerfilUsuario.CLIENTE);
        criarUsuario("cliente.dois", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);
        compraService.criar(
                "cliente.um", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 1))));
        compraService.criar(
                "cliente.dois", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 1))));

        mockMvc.perform(get("/api/compras").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/compras")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": [{"produtoId": %d, "quantidade": 1}]}
                                """.formatted(produto.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void resultadoRejeitadoDuplicadoLiberaEstoqueUmaUnicaVez() {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);
        CompraResponse compra = compraService.criar(
                "cliente", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 3))));
        PaymentResult rejeitado = new PaymentResult(
                compra.id(), compra.correlationId(), compra.total(), compra.moeda(), PaymentStatus.REJECTED);

        assertThat(compraService.processarResultadoPagamento(rejeitado))
                .isEqualTo(PaymentProcessingOutcome.APPLIED);
        assertThat(compraService.processarResultadoPagamento(rejeitado))
                .isEqualTo(PaymentProcessingOutcome.DUPLICATE_IGNORED);

        assertThat(compraRepository.findById(compra.id()).orElseThrow().getStatus())
                .isEqualTo(CompraStatus.REJECTED);
        assertThat(mercadoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(10);
    }

    @Test
    void resultadoConflitanteNaoAlteraStatusNemEstoque() {
        criarUsuario("cliente", PerfilUsuario.CLIENTE);
        Mercado produto = criarProduto("Cafe", "12.50", "7.25", 10);
        CompraResponse compra = compraService.criar(
                "cliente", new CompraCreateRequest(List.of(new CompraItemRequest(produto.getId(), 3))));
        PaymentResult aprovado = new PaymentResult(
                compra.id(), compra.correlationId(), compra.total(), compra.moeda(), PaymentStatus.APPROVED);
        PaymentResult rejeitado = new PaymentResult(
                compra.id(), compra.correlationId(), compra.total(), compra.moeda(), PaymentStatus.REJECTED);

        assertThat(compraService.processarResultadoPagamento(aprovado))
                .isEqualTo(PaymentProcessingOutcome.APPLIED);
        assertThatThrownBy(() -> compraService.processarResultadoPagamento(rejeitado))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class);

        assertThat(compraRepository.findById(compra.id()).orElseThrow().getStatus())
                .isEqualTo(CompraStatus.APPROVED);
        assertThat(mercadoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(7);
    }

    private Usuario criarUsuario(String login, PerfilUsuario perfil) {
        return usuarioRepository.save(Usuario.builder()
                .nome("Usuario " + login)
                .login(login)
                .senha(passwordEncoder.encode("senha-segura"))
                .ativo(true)
                .perfil(perfil)
                .build());
    }

    private Mercado criarProduto(String nome, String preco, String custo, int estoque) {
        return mercadoRepository.save(Mercado.builder()
                .nome(nome)
                .tipo("Alimento")
                .setor("Mercearia")
                .tamanho("1 unidade")
                .preco(new BigDecimal(preco))
                .custoAquisicao(new BigDecimal(custo))
                .quantidadeEstoque(estoque)
                .build());
    }
}
