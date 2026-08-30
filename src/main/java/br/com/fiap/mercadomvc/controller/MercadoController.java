package br.com.fiap.mercadomvc.controller;

import br.com.fiap.mercadomvc.exception.MercadoNaoEncontradoException;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.service.MercadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mercado")
@RequiredArgsConstructor
public class MercadoController {

    private final MercadoService mercadoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", mercadoService.listarTodos());
        return "mercado/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("mercado", new Mercado());
        model.addAttribute("modoEdicao", false);
        return "mercado/formulario";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("mercado") Mercado mercado,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicao", false);
            return "mercado/formulario";
        }

        mercadoService.criar(mercado);
        redirectAttributes.addFlashAttribute("sucesso", "Produto cadastrado com sucesso.");
        return "redirect:/mercado";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("mercado", mercadoService.buscarPorId(id));
        return "mercado/detalhes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("mercado", mercadoService.buscarPorId(id));
        model.addAttribute("modoEdicao", true);
        return "mercado/formulario";
    }

    @PutMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("mercado") Mercado mercado,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            mercado.setId(id);
            model.addAttribute("modoEdicao", true);
            return "mercado/formulario";
        }

        mercadoService.atualizar(id, mercado);
        redirectAttributes.addFlashAttribute("sucesso", "Produto atualizado com sucesso.");
        return "redirect:/mercado/" + id;
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        mercadoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Produto excluido com sucesso.");
        return "redirect:/mercado";
    }

    @ExceptionHandler(MercadoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String produtoNaoEncontrado(Model model) {
        model.addAttribute("mensagem", "Produto nao encontrado.");
        return "mercado/nao-encontrado";
    }
}
