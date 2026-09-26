package br.edu.hortifruti.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "DATABASE_URL")
public class PostgreSqlDataSourceConfig {

    @Bean
    DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        URI uri = URI.create(databaseUrl);

        if (!"postgresql".equals(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("DATABASE_URL deve utilizar o formato postgresql://usuario:senha@host:porta/banco.");
        }

        String[] credenciais = extrairCredenciais(uri);
        String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
        String porta = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String parametros = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String urlJdbc = "jdbc:postgresql://" + host + porta + uri.getRawPath() + parametros;

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(urlJdbc)
                .username(credenciais[0])
                .password(credenciais[1])
                .build();
    }

    private String[] extrairCredenciais(URI uri) {
        String credenciais = uri.getRawUserInfo();
        int separador = credenciais == null ? -1 : credenciais.indexOf(':');

        if (separador <= 0) {
            throw new IllegalArgumentException("DATABASE_URL deve informar usuário e senha.");
        }

        return new String[] {
                decodificar(credenciais.substring(0, separador)),
                decodificar(credenciais.substring(separador + 1))
        };
    }

    private String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }
}
