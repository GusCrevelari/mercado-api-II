package br.com.fiap.mercadomvc.api;

import br.com.fiap.mercadomvc.api.dto.MercadoResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MercadoModelAssembler implements RepresentationModelAssembler<MercadoResponse, EntityModel<MercadoResponse>> {

    @Override
    public EntityModel<MercadoResponse> toModel(MercadoResponse mercado) {
        return EntityModel.of(mercado,
                linkTo(methodOn(MercadoRestController.class).buscarPorId(mercado.id())).withSelfRel(),
                linkTo(methodOn(MercadoRestController.class).listar()).withRel("collection"));
    }
}
