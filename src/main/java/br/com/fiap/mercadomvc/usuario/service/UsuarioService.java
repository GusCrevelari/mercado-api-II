package br.com.fiap.mercadomvc.usuario.service;

import br.com.fiap.mercadomvc.usuario.dto.UsuarioRegistroForm;
import br.com.fiap.mercadomvc.usuario.exception.UsuarioJaExisteException;
import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("postgres")
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarCliente(UsuarioRegistroForm form) {
        String login = normalizarLogin(form.getLogin());
        if (usuarioRepository.existsByLoginIgnoreCase(login)) {
            throw new UsuarioJaExisteException();
        }

        Usuario usuario = Usuario.builder()
                .nome(form.getNome().trim())
                .login(login)
                .senha(passwordEncoder.encode(form.getSenha()))
                .ativo(true)
                .perfil(PerfilUsuario.CLIENTE)
                .build();

        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new UsuarioJaExisteException();
        }
    }

    public static String normalizarLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }
}
