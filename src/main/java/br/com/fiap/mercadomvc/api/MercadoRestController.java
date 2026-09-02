package br.com.fiap.mercadomvc.api;

import br.com.fiap.mercadomvc.api.dto.MercadoCreateRequest;
import br.com.fiap.mercadomvc.api.dto.MercadoPatchRequest;
import br.com.fiap.mercadomvc.api.dto.MercadoResponse;
import br.com.fiap.mercadomvc.api.dto.MercadoUpdateRequest;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.service.MercadoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/mercado")
@RequiredArgsConstructor
public class MercadoRestController {

    private final MercadoService mercadoService;
    private final MercadoRestMapper mapper;
    private final MercadoModelAssembler assembler;

    @GetMapping
    public CollectionModel<EntityModel<MercadoResponse>> listar() {
        List<EntityModel<MercadoResponse>> produtos = mercadoService.listarTodos().stream()
                .map(mapper::toResponse)
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(produtos,
                linkTo(methodOn(MercadoRestController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<MercadoResponse> buscarPorId(@PathVariable Long id) {
        return assembler.toModel(mapper.toResponse(mercadoService.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<EntityModel<MercadoResponse>> criar(@Valid @RequestBody MercadoCreateRequest request) {
        Mercado criado = mercadoService.criar(mapper.toEntity(request));
        EntityModel<MercadoResponse> response = assembler.toModel(mapper.toResponse(criado));
        URI location = linkTo(methodOn(MercadoRestController.class).buscarPorId(criado.getId())).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public EntityModel<MercadoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody MercadoUpdateRequest request) {
        Mercado atualizado = mercadoService.atualizar(id, mapper.toEntity(request));
        return assembler.toModel(mapper.toResponse(atualizado));
    }

    @PatchMapping("/{id}")
    public EntityModel<MercadoResponse> atualizarParcial(
            @PathVariable Long id,
            @RequestBody MercadoPatchRequest request) {
        Mercado atualizado = mercadoService.atualizarParcial(id, mercado -> mapper.applyPatch(request, mercado));
        return assembler.toModel(mapper.toResponse(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        mercadoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
