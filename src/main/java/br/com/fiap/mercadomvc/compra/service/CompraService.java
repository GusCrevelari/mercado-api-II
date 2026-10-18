package br.com.fiap.mercadomvc.compra.service;

import br.com.fiap.mercadomvc.compra.api.CompraRestMapper;
import br.com.fiap.mercadomvc.compra.dto.CompraCreateRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraItemRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.exception.CompraAcessoNegadoException;
import br.com.fiap.mercadomvc.compra.exception.CompraNaoEncontradaException;
import br.com.fiap.mercadomvc.compra.exception.CompraValidationException;
import br.com.fiap.mercadomvc.compra.exception.EstoqueInsuficienteException;
import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.compra.model.CompraItem;
import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentProcessingOutcome;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentRequest;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentResult;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutbox;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxRepository;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.exception.MercadoNaoEncontradoException;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import br.com.fiap.mercadomvc.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompraService {

    public static final String DEFAULT_CURRENCY = "BRL";

    private final CompraRepository compraRepository;
    private final MercadoRepository mercadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraRestMapper mapper;
    private final PaymentOutboxRepository paymentOutboxRepository;

    @Transactional
    public CompraResponse criar(String loginAutenticado, CompraCreateRequest request) {
        Usuario cliente = usuarioRepository.findByLoginIgnoreCase(UsuarioService.normalizarLogin(loginAutenticado))
                .orElseThrow(() -> new CompraValidationException("Cliente autenticado nao encontrado."));

        Map<Long, Integer> quantidades = validarEOrganizarItens(request.itens());
        List<Long> idsOrdenados = quantidades.keySet().stream().sorted().toList();
        List<Mercado> produtos = mercadoRepository.buscarTodosPorIdComBloqueio(idsOrdenados);
        Map<Long, Mercado> produtosPorId = produtos.stream()
                .collect(Collectors.toMap(Mercado::getId, Function.identity()));

        idsOrdenados.stream()
                .filter(id -> !produtosPorId.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new MercadoNaoEncontradoException(id);
                });

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal custoTotal = BigDecimal.ZERO;
        Map<Mercado, CompraItem> itens = new LinkedHashMap<>();

        for (Long produtoId : idsOrdenados) {
            Mercado produto = produtosPorId.get(produtoId);
            int quantidade = quantidades.get(produtoId);
            if (produto.getQuantidadeEstoque() < quantidade) {
                throw new EstoqueInsuficienteException(
                        produtoId, quantidade, produto.getQuantidadeEstoque());
            }

            BigDecimal subtotal = multiplicar(produto.getPreco(), quantidade);
            BigDecimal subtotalCusto = multiplicar(produto.getCustoAquisicao(), quantidade);
            produto.reservarEstoque(quantidade);
            total = total.add(subtotal);
            custoTotal = custoTotal.add(subtotalCusto);
            itens.put(produto, new CompraItem(
                    produto,
                    quantidade,
                    produto.getPreco(),
                    produto.getCustoAquisicao(),
                    subtotal,
                    subtotalCusto));
        }

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        Compra compra = new Compra(
                cliente,
                dinheiro(total),
                dinheiro(custoTotal),
                DEFAULT_CURRENCY,
                UUID.randomUUID(),
                agora);
        itens.values().forEach(compra::adicionarItem);

        Compra salva = compraRepository.saveAndFlush(compra);
        paymentOutboxRepository.save(new PaymentOutbox(salva, UUID.randomUUID(), agora));
        return mapper.toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar(String loginAutenticado, boolean administrador) {
        List<Compra> compras = administrador
                ? compraRepository.findAllByOrderByCriadoEmDesc()
                : compraRepository.findAllByClienteLoginIgnoreCaseOrderByCriadoEmDesc(loginAutenticado);
        return compras.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse buscarPorId(Long id, String loginAutenticado, boolean administrador) {
        Compra compra = compraRepository.buscarPorIdComDetalhes(id)
                .orElseThrow(() -> new CompraNaoEncontradaException(id));
        if (!administrador && !compra.getCliente().getLogin().equalsIgnoreCase(loginAutenticado)) {
            throw new CompraAcessoNegadoException();
        }
        return mapper.toResponse(compra);
    }

    @Transactional(readOnly = true)
    public PaymentRequest criarPaymentRequest(Long compraId) {
        Compra compra = compraRepository.buscarPorIdComDetalhes(compraId)
                .orElseThrow(() -> new CompraNaoEncontradaException(compraId));
        if (compra.getStatus() != CompraStatus.PENDING_PAYMENT) {
            throw new ResultadoPagamentoInvalidoException("Somente compras pendentes podem solicitar pagamento.");
        }
        return new PaymentRequest(
                compra.getId(),
                compra.getCorrelationId(),
                compra.getTotal(),
                compra.getMoeda());
    }

    @Transactional
    public PaymentProcessingOutcome processarResultadoPagamento(PaymentResult result) {
        validarResultado(result);
        Compra compra = compraRepository.buscarPorIdComBloqueio(result.purchaseId())
                .orElseThrow(() -> new CompraNaoEncontradaException(result.purchaseId()));
        validarContratoResultado(compra, result);

        CompraStatus statusDestino = result.status() == PaymentStatus.APPROVED
                ? CompraStatus.APPROVED
                : CompraStatus.REJECTED;

        if (compra.getStatus() == statusDestino) {
            return PaymentProcessingOutcome.DUPLICATE_IGNORED;
        }
        if (compra.getStatus() != CompraStatus.PENDING_PAYMENT) {
            throw new ResultadoPagamentoInvalidoException(
                    "A compra ja possui resultado de pagamento diferente.");
        }

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        if (statusDestino == CompraStatus.APPROVED) {
            compra.aprovar(agora);
            return PaymentProcessingOutcome.APPLIED;
        }

        List<Long> produtoIds = compra.getItens().stream()
                .map(item -> item.getProduto().getId())
                .sorted()
                .toList();
        Map<Long, Mercado> produtos = mercadoRepository.buscarTodosPorIdComBloqueio(produtoIds).stream()
                .collect(Collectors.toMap(Mercado::getId, Function.identity()));
        for (CompraItem item : compra.getItens()) {
            Mercado produto = produtos.get(item.getProduto().getId());
            if (produto == null) {
                throw new ResultadoPagamentoInvalidoException(
                        "Produto da compra nao esta mais disponivel para liberar a reserva.");
            }
            produto.liberarEstoque(item.getQuantidade());
        }
        compra.rejeitar(agora);
        return PaymentProcessingOutcome.APPLIED;
    }

    private Map<Long, Integer> validarEOrganizarItens(List<CompraItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CompraValidationException("Informe ao menos um item.");
        }
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        Set<Long> produtosDuplicados = new HashSet<>();
        for (CompraItemRequest item : itens) {
            if (item == null || item.produtoId() == null || item.quantidade() == null || item.quantidade() <= 0) {
                throw new CompraValidationException("Todos os itens devem ter produto e quantidade positiva.");
            }
            if (quantidades.putIfAbsent(item.produtoId(), item.quantidade()) != null) {
                produtosDuplicados.add(item.produtoId());
            }
        }
        if (!produtosDuplicados.isEmpty()) {
            throw new CompraValidationException("Cada produto deve aparecer uma unica vez na compra.");
        }
        return quantidades;
    }

    private void validarResultado(PaymentResult result) {
        if (result == null
                || result.purchaseId() == null
                || result.correlationId() == null
                || result.amount() == null
                || result.currency() == null
                || result.status() == null) {
            throw new ResultadoPagamentoInvalidoException("Resultado de pagamento incompleto.");
        }
    }

    private void validarContratoResultado(Compra compra, PaymentResult result) {
        if (!compra.getCorrelationId().equals(result.correlationId())) {
            throw new ResultadoPagamentoInvalidoException("Correlation identifier nao corresponde a compra.");
        }
        if (compra.getTotal().compareTo(result.amount()) != 0) {
            throw new ResultadoPagamentoInvalidoException("Valor do pagamento nao corresponde a compra.");
        }
        if (!compra.getMoeda().equals(result.currency())) {
            throw new ResultadoPagamentoInvalidoException("Moeda do pagamento nao corresponde a compra.");
        }
    }

    private BigDecimal multiplicar(BigDecimal valor, int quantidade) {
        return dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
