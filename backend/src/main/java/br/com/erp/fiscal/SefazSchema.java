package br.com.erp.fiscal;
import java.nio.file.*;
import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.ls.*;

/** Validates an installed official schema bundle; imports are confined to its directory. */
public final class SefazSchema {
 private final Path root;
 public SefazSchema(Path root){try{this.root=root.toRealPath();if(!Files.isDirectory(this.root))throw new IOException();}catch(IOException e){throw new IllegalArgumentException("Pacote oficial de schemas fiscais não instalado.");}}
 public void validate(String xml,String schemaFile){try{var factory=SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");factory.setResourceResolver((type,namespace,publicId,systemId,baseURI)->{try{Path base=baseURI==null?root:Path.of(java.net.URI.create(baseURI)).getParent();Path child=confined(base.resolve(systemId));return new LocalInput(publicId,child.toUri().toString(),child.getParent().toUri().toString(),Files.newInputStream(child));}catch(Exception e){throw new IllegalArgumentException("Importação de schema fiscal fora do pacote instalado.");}});var schema=factory.newSchema(confined(root.resolve(schemaFile)).toFile());var validator=schema.newValidator();validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");SefazXml.parse(xml);validator.validate(new StreamSource(new StringReader(xml)));}catch(Exception e){throw new IllegalArgumentException("XML fiscal incompatível com o pacote oficial de schemas instalado.");}}
 private Path confined(Path file)throws IOException{Path resolved=file.toRealPath();if(!resolved.startsWith(root)||!Files.isRegularFile(resolved))throw new IOException();return resolved;}
 private static class LocalInput implements LSInput {
  private String publicId,systemId,baseUri,encoding;private InputStream bytes;private Reader reader;private String data;private boolean certified;
  LocalInput(String publicId,String systemId,String baseUri,InputStream bytes){this.publicId=publicId;this.systemId=systemId;this.baseUri=baseUri;this.bytes=bytes;}
  public Reader getCharacterStream(){return reader;}public void setCharacterStream(Reader r){reader=r;}public InputStream getByteStream(){return bytes;}public void setByteStream(InputStream in){bytes=in;}public String getStringData(){return data;}public void setStringData(String s){data=s;}public String getSystemId(){return systemId;}public void setSystemId(String s){systemId=s;}public String getPublicId(){return publicId;}public void setPublicId(String s){publicId=s;}public String getBaseURI(){return baseUri;}public void setBaseURI(String s){baseUri=s;}public String getEncoding(){return encoding;}public void setEncoding(String s){encoding=s;}public boolean getCertifiedText(){return certified;}public void setCertifiedText(boolean b){certified=b;}
 }
}
