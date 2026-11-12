package br.com.erp.product;

import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Barcode(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String code,@NotNull Kind kind) {
    public enum Kind { GTIN, INTERNAL }
    public String canonical() {
        if (kind==Kind.INTERNAL) return "INTERNAL:"+code;
        if (!code.matches("(?:[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})")) throw invalid();
        int sum=0,weight=3;
        for (int i=code.length()-2;i>=0;i--) { sum+=(code.charAt(i)-'0')*weight; weight=4-weight; }
        if ((10-sum%10)%10!=code.charAt(code.length()-1)-'0') throw invalid();
        return "GTIN:"+"0".repeat(14-code.length())+code;
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"GTIN inválido: confira tamanho e dígito verificador."); }
}
