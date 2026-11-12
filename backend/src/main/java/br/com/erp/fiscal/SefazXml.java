package br.com.erp.fiscal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.*;

/** XML parsing is local and never loads DTDs, entities or network resources. */
public final class SefazXml {
 public static final String NFE="http://www.portalfiscal.inf.br/nfe",SOAP="http://www.w3.org/2003/05/soap-envelope";
 public static final int MAX_BYTES=2_000_000;
 private SefazXml(){}
 public static Document parse(String xml){if(xml==null||xml.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("XML fiscal ausente ou muito grande.");try{var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);f.setXIncludeAware(false);f.setExpandEntityReferences(false);f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");var b=f.newDocumentBuilder();b.setErrorHandler(new ErrorHandler(){public void warning(SAXParseException e)throws SAXException{throw e;}public void error(SAXParseException e)throws SAXException{throw e;}public void fatalError(SAXParseException e)throws SAXException{throw e;}});return b.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalArgumentException("XML fiscal inválido ou contendo recurso externo.");}}
 public static String text(Node node){try{var f=TransformerFactory.newInstance();f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");var t=f.newTransformer();t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");var out=new StringWriter();t.transform(new DOMSource(node),new StreamResult(out));return out.toString();}catch(Exception e){throw new IllegalArgumentException("Não foi possível serializar XML fiscal.");}}
 public static Element unique(Document doc,String namespace,String name){var nodes=doc.getElementsByTagNameNS(namespace,name);if(nodes.getLength()!=1)throw new IllegalArgumentException("XML deve conter exatamente um elemento "+name+".");return (Element)nodes.item(0);}
 public static String field(Element root,String name){var nodes=root.getElementsByTagNameNS(NFE,name);if(nodes.getLength()!=1)throw new IllegalArgumentException("Campo fiscal ausente ou repetido: "+name);return nodes.item(0).getTextContent();}
 public static String soap(SefazEndpoints.Service service,String payload){var request=parse(payload);var doc=parse("<soap:Envelope xmlns:soap=\""+SOAP+"\"><soap:Body/></soap:Envelope>");var body=unique(doc,SOAP,"Body");var data=doc.createElementNS(service.namespace(),"nfeDadosMsg");data.appendChild(doc.importNode(request.getDocumentElement(),true));body.appendChild(data);return text(doc);}
 public static String response(String soap){var d=parse(soap);var body=unique(d,SOAP,"Body");var elements=new ArrayList<Element>();for(var n=body.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e)elements.add(e);if(elements.size()!=1)throw new IllegalArgumentException("Resposta SOAP inválida.");var response=elements.getFirst();if(SOAP.equals(response.getNamespaceURI())&&"Fault".equals(response.getLocalName()))throw new IllegalArgumentException("SEFAZ retornou falha SOAP. Consulte o histórico da operação.");if(NFE.equals(response.getNamespaceURI()))return text(response);var list=new ArrayList<Element>();for(var n=response.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e)list.add(e);if(list.size()!=1||!NFE.equals(list.getFirst().getNamespaceURI()))throw new IllegalArgumentException("Resposta fiscal ausente no envelope SOAP.");return text(list.getFirst());}
}
