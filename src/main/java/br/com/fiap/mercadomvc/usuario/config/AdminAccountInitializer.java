package br.com.fiap.mercadomvc.usuario.config;

import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import br.com.fiap.mercadomvc.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("postgres")
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:}")
    private String username;

    @Value("${app.security.admin.password:}")
    private String password;

    @Value("${app.security.admin.name:Administrador}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);

        if (!hasUsername && !hasPassword) {
            return;
        }
        if (!hasUsername || !hasPassword) {
            throw new IllegalStateException(
                    "APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD devem ser informados em conjunto.");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD deve ter entre 8 e 72 caracteres.");
        }

        String normalizedLogin = UsuarioService.normalizarLogin(username);
        if (normalizedLogin.length() < 3 || normalizedLogin.length() > 100) {
            throw new IllegalStateException("APP_ADMIN_USERNAME deve ter entre 3 e 100 caracteres.");
        }
        String adminName = StringUtils.hasText(name) ? name.trim() : "Administrador";
        if (adminName.length() > 120) {
            throw new IllegalStateException("APP_ADMIN_NAME deve ter no maximo 120 caracteres.");
        }

        usuarioRepository.findByLoginIgnoreCase(normalizedLogin).ifPresentOrElse(existing -> {
            if (existing.getPerfil() != PerfilUsuario.ADMIN) {
                throw new IllegalStateException(
                        "O login configurado para o ADMIN ja pertence a uma conta sem privilegios administrativos.");
            }
        }, () -> usuarioRepository.save(Usuario.builder()
                .nome(adminName)
                .login(normalizedLogin)
                .senha(passwordEncoder.encode(password))
                .ativo(true)
                .perfil(PerfilUsuario.ADMIN)
                .build()));
    }
}
