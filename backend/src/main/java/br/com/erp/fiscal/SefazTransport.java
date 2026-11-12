package br.com.erp.fiscal;

import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.*;

/** Native SOAP 1.2/mTLS transport. No intermediary, no disabled TLS checks, no automatic POST retries. */
public final class SefazTransport {
 private final HttpClient client;
 public SefazTransport(KeyStore a1,char[] password){try{var keys=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());keys.init(a1,password);var tls=SSLContext.getInstance("TLS");tls.init(keys.getKeyManagers(),null,null);client=HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();}catch(Exception e){throw new IllegalArgumentException("Não foi possível configurar o certificado para SEFAZ.");}}
 public String exchange(FiscalContext.Environment environment,SefazEndpoints.Service service,String xml)throws IOException,InterruptedException{
  var request=HttpRequest.newBuilder(SefazEndpoints.go(environment,service)).timeout(Duration.ofSeconds(45)).header("Content-Type","application/soap+xml; charset=utf-8; action=\""+service.action()+"\"").POST(HttpRequest.BodyPublishers.ofString(SefazXml.soap(service,xml),StandardCharsets.UTF_8)).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());try(var in=response.body()){byte[] data=in.readNBytes(SefazXml.MAX_BYTES+1);if(data.length>SefazXml.MAX_BYTES)throw new IOException("Resposta da SEFAZ excede o limite permitido.");if(response.statusCode()!=200)throw new IOException("SEFAZ retornou HTTP "+response.statusCode()+". Consulte a operação antes de reenviar.");return SefazXml.response(new String(data,StandardCharsets.UTF_8));}
 }
}
