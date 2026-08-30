package br.com.fiap.mercadomvc.service;

import br.com.fiap.mercadomvc.exception.MercadoNaoEncontradoException;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MercadoService {

    private final MercadoRepository mercadoRepository;

    @Transactional(readOnly = true)
    public List<Mercado> listarTodos() {
        return mercadoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Mercado buscarPorId(Long id) {
        return mercadoRepository.findById(id)
                .orElseThrow(() -> new MercadoNaoEncontradoException(id));
    }

    @Transactional
    public Mercado criar(Mercado mercado) {
        mercado.setId(null);
        return mercadoRepository.save(mercado);
    }

    @Transactional
    public Mercado atualizar(Long id, Mercado mercadoAtualizado) {
        Mercado mercado = buscarPorId(id);
        mercado.setNome(mercadoAtualizado.getNome());
        mercado.setTipo(mercadoAtualizado.getTipo());
        mercado.setSetor(mercadoAtualizado.getSetor());
        mercado.setTamanho(mercadoAtualizado.getTamanho());
        mercado.setPreco(mercadoAtualizado.getPreco());
        return mercadoRepository.save(mercado);
    }

    @Transactional
    public void excluir(Long id) {
        Mercado mercado = buscarPorId(id);
        mercadoRepository.delete(mercado);
    }
}
