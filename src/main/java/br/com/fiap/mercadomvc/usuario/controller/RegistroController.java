package br.com.fiap.mercadomvc.usuario.controller;

import br.com.fiap.mercadomvc.usuario.dto.UsuarioRegistroForm;
import br.com.fiap.mercadomvc.usuario.exception.UsuarioJaExisteException;
import br.com.fiap.mercadomvc.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Profile("postgres")
@RequiredArgsConstructor
public class RegistroController {

    private final UsuarioService usuarioService;

    @GetMapping("/registro")
    public String formulario(Model model) {
        model.addAttribute("usuario", new UsuarioRegistroForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @Valid @ModelAttribute("usuario") UsuarioRegistroForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "registro";
        }

        try {
            usuarioService.registrarCliente(form);
        } catch (UsuarioJaExisteException ex) {
            bindingResult.rejectValue("login", "usuario.duplicado", ex.getMessage());
            return "registro";
        }

        return "redirect:/login?registered";
    }
}
