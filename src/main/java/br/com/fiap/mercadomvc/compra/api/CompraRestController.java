package br.com.fiap.mercadomvc.compra.api;

import br.com.fiap.mercadomvc.compra.dto.CompraCreateRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.service.CompraService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraRestController {

    private final CompraService compraService;

    @PostMapping
    public ResponseEntity<CompraResponse> criar(
            @Valid @RequestBody CompraCreateRequest request,
            Authentication authentication) {
        CompraResponse criada = compraService.criar(authentication.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public List<CompraResponse> listar(Authentication authentication) {
        return compraService.listar(authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public CompraResponse buscarPorId(@PathVariable Long id, Authentication authentication) {
        return compraService.buscarPorId(id, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
