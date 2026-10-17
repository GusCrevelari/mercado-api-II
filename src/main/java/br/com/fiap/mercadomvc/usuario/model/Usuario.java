package br.com.fiap.mercadomvc.usuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CP5_USUARIO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cp5_seq_usuario")
    @SequenceGenerator(name = "cp5_seq_usuario", sequenceName = "CP5_SEQ_USUARIO", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "LOGIN", nullable = false, unique = true, length = 100)
    private String login;

    @Column(name = "SENHA", nullable = false, length = 100)
    private String senha;

    @Column(name = "ATIVO", nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 20)
    private PerfilUsuario perfil;
}
