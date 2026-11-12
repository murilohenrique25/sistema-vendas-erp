package br.com.erp.fiscal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.*;
import org.w3c.dom.*;

/** Implements the NF-e enveloped XMLDSig profile. Does not attest tax correctness or SEFAZ authorization. */
public final class SefazSignature {
 private SefazSignature(){}
 public static String sign(String xml,String elementName,PrivateKey key,X509Certificate certificate){
  var doc=SefazXml.parse(xml);var target=SefazXml.unique(doc,SefazXml.NFE,elementName);String id=target.getAttribute("Id");if(id.isBlank()||!id.matches("[A-Za-z0-9]{10,80}"))throw new IllegalArgumentException("Identificador fiscal inválido.");if(doc.getElementsByTagNameNS(XMLSignature.XMLNS,"Signature").getLength()!=0)throw new IllegalArgumentException("XML já contém assinatura.");
  int matches=0;var nodes=doc.getElementsByTagName("*");for(int i=0;i<nodes.getLength();i++)if(id.equals(((Element)nodes.item(i)).getAttribute("Id")))matches++;if(matches!=1)throw new IllegalArgumentException("Identificador fiscal duplicado.");
  try{certificate.checkValidity();target.setIdAttribute("Id",true);var factory=XMLSignatureFactory.getInstance("DOM");var transforms=List.of(factory.newTransform(Transform.ENVELOPED,(TransformParameterSpec)null),factory.newTransform(CanonicalizationMethod.INCLUSIVE,(TransformParameterSpec)null));var reference=factory.newReference("#"+id,factory.newDigestMethod(DigestMethod.SHA1,null),transforms,null,null);var signedInfo=factory.newSignedInfo(factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,(C14NMethodParameterSpec)null),factory.newSignatureMethod(SignatureMethod.RSA_SHA1,null),List.of(reference));var info=factory.getKeyInfoFactory();var keyInfo=info.newKeyInfo(List.of(info.newX509Data(List.of(certificate))));var context=new DOMSignContext(key,target.getParentNode());factory.newXMLSignature(signedInfo,keyInfo).sign(context);return SefazXml.text(doc);}catch(Exception e){throw new IllegalArgumentException("Falha ao assinar o XML fiscal com A1.");}
 }
}
