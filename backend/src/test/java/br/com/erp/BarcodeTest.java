package br.com.erp;

import br.com.erp.product.Barcode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;

class BarcodeTest {
    @ParameterizedTest @ValueSource(strings={"96385074","036000291452","4006381333931","04006381333931"})
    void validatesAllSupportedLengths(String code) { assertEquals(19,new Barcode(code,Barcode.Kind.GTIN).canonical().length()); }
    @ParameterizedTest @ValueSource(strings={"96385075","036000291453","4006381333932","04006381333932","123","ABC12345"})
    void rejectsInvalidGtin(String code) { assertThrows(ResponseStatusException.class,()->new Barcode(code,Barcode.Kind.GTIN).canonical()); }
}
