package br.com.fiap.mercadomvc.usuario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRegistroForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o usuario.")
    @Size(min = 3, max = 100, message = "O usuario deve ter entre 3 e 100 caracteres.")
    private String login;

    @NotBlank(message = "Informe o email.")
    @Email(message = "Informe um email valido.")
    @Size(max = 254, message = "O email deve ter no maximo 254 caracteres.")
    private String email;

    @NotBlank(message = "Informe a senha.")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
    private String senha;

    @NotBlank(message = "Confirme a senha.")
    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas devem ser iguais.")
    public boolean isSenhaConfirmada() {
        return senha != null && senha.equals(confirmacaoSenha);
    }
}
