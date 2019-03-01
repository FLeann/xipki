/*
 *
 * Copyright (c) 2013 - 2019 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.api.profile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUniversalString;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.DirectoryString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.math.ec.ECCurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.BadCertTemplateException;
import org.xipki.ca.api.profile.KeyParametersOption.AllowAllParametersOption;
import org.xipki.ca.api.profile.KeyParametersOption.DSAParametersOption;
import org.xipki.ca.api.profile.KeyParametersOption.ECParamatersOption;
import org.xipki.ca.api.profile.KeyParametersOption.RSAParametersOption;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.security.util.AlgorithmUtil;
import org.xipki.security.util.X509Util;
import org.xipki.util.CollectionUtil;
import org.xipki.util.LogUtil;
import org.xipki.util.LruCache;
import org.xipki.util.Args;
import org.xipki.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class BaseCertprofile extends Certprofile {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCertprofile.class);

  private static LruCache<ASN1ObjectIdentifier, Integer> ecCurveFieldSizes = new LruCache<>(100);

  protected BaseCertprofile() {
  }

  public abstract Map<ASN1ObjectIdentifier, KeyParametersOption> getKeyAlgorithms();

  /**
   * Get the SubjectControl.
   *
   * @return the SubjectControl, may not be <code>null</code>.
   */
  protected abstract SubjectControl getSubjectControl();

  @Override
  public Date getNotBefore(Date requestedNotBefore) {
    Date now = new Date();
    return (requestedNotBefore != null && requestedNotBefore.after(now)) ? requestedNotBefore : now;
  }

  @Override
  public SubjectInfo getSubject(X500Name requestedSubject)
      throws CertprofileException, BadCertTemplateException {
    Args.notNull(requestedSubject, "requestedSubject");

    verifySubjectDnOccurence(requestedSubject);

    RDN[] requstedRdns = requestedSubject.getRDNs();
    SubjectControl scontrol = getSubjectControl();

    List<RDN> rdns = new LinkedList<>();

    for (ASN1ObjectIdentifier type : scontrol.getTypes()) {
      RdnControl control = scontrol.getControl(type);
      if (control == null) {
        continue;
      }

      RDN[] thisRdns = getRdns(requstedRdns, type);
      if (thisRdns == null) {
        continue;
      }
      int len = thisRdns.length;
      if (len == 0) {
        continue;
      }

      if (ObjectIdentifiers.DN_EmailAddress.equals(type)) {
        throw new BadCertTemplateException("emailAddress is not allowed");
      }

      if (len == 1) {
        ASN1Encodable rdnValue = thisRdns[0].getFirst().getValue();
        RDN rdn;
        if (ObjectIdentifiers.DN_DATE_OF_BIRTH.equals(type)) {
          rdn = createDateOfBirthRdn(type, rdnValue);
        } else if (ObjectIdentifiers.DN_POSTAL_ADDRESS.equals(type)) {
          rdn = createPostalAddressRdn(type, rdnValue, control, 0);
        } else {
          String value = X509Util.rdnValueToString(rdnValue);
          rdn = createSubjectRdn(value, type, control, 0);
        }

        if (rdn != null) {
          rdns.add(rdn);
        }
      } else {
        if (ObjectIdentifiers.DN_DATE_OF_BIRTH.equals(type)) {
          for (int i = 0; i < len; i++) {
            RDN rdn = createDateOfBirthRdn(type, thisRdns[i].getFirst().getValue());
            rdns.add(rdn);
          }
        } else if (ObjectIdentifiers.DN_POSTAL_ADDRESS.equals(type)) {
          for (int i = 0; i < len; i++) {
            RDN rdn = createPostalAddressRdn(type, thisRdns[i].getFirst().getValue(),
                control, i);
            rdns.add(rdn);
          }
        } else {
          String[] values = new String[len];
          for (int i = 0; i < len; i++) {
            values[i] = X509Util.rdnValueToString(thisRdns[i].getFirst().getValue());
          }

          int idx = 0;
          for (String value : values) {
            rdns.add(createSubjectRdn(value, type, control, idx++));
          }
        } // if
      } // if
    } // for

    Set<String> subjectDnGroups = scontrol.getGroups();
    if (CollectionUtil.isNonEmpty(subjectDnGroups)) {
      Set<String> consideredGroups = new HashSet<>();
      final int n = rdns.size();

      List<RDN> newRdns = new ArrayList<>(rdns.size());
      for (int i = 0; i < n; i++) {
        RDN rdn = rdns.get(i);
        ASN1ObjectIdentifier type = rdn.getFirst().getType();
        String group = scontrol.getGroup(type);
        if (group == null) {
          newRdns.add(rdn);
        } else if (!consideredGroups.contains(group)) {
          List<AttributeTypeAndValue> atvs = new LinkedList<>();
          atvs.add(rdn.getFirst());
          for (int j = i + 1; j < n; j++) {
            RDN rdn2 = rdns.get(j);
            ASN1ObjectIdentifier type2 = rdn2.getFirst().getType();
            String group2 = scontrol.getGroup(type2);
            if (group.equals(group2)) {
              atvs.add(rdn2.getFirst());
            }
          }

          newRdns.add(new RDN(atvs.toArray(new AttributeTypeAndValue[0])));
          consideredGroups.add(group);
        }
      } // for

      rdns = newRdns;
    } // if

    X500Name grantedSubject = new X500Name(rdns.toArray(new RDN[0]));
    return new SubjectInfo(grantedSubject, null);
  } // method getSubject

  @Override
  public boolean incSerialNumberIfSubjectExists() {
    return false;
  }

  @Override
  public SubjectPublicKeyInfo checkPublicKey(SubjectPublicKeyInfo publicKey)
      throws CertprofileException, BadCertTemplateException {
    Args.notNull(publicKey, "publicKey");

    Map<ASN1ObjectIdentifier, KeyParametersOption> keyAlgorithms = getKeyAlgorithms();
    if (CollectionUtil.isEmpty(keyAlgorithms)) {
      return publicKey;
    }

    ASN1ObjectIdentifier keyType = publicKey.getAlgorithm().getAlgorithm();
    if (!keyAlgorithms.containsKey(keyType)) {
      throw new BadCertTemplateException("key type " + keyType.getId() + " is not permitted");
    }

    KeyParametersOption keyParamsOption = keyAlgorithms.get(keyType);
    if (keyParamsOption instanceof AllowAllParametersOption) {
      return publicKey;
    } else if (keyParamsOption instanceof ECParamatersOption) {
      ECParamatersOption ecOption = (ECParamatersOption) keyParamsOption;
      // parameters
      ASN1Encodable algParam = publicKey.getAlgorithm().getParameters();
      ASN1ObjectIdentifier curveOid;

      if (algParam instanceof ASN1ObjectIdentifier) {
        curveOid = (ASN1ObjectIdentifier) algParam;
        if (!ecOption.allowsCurve(curveOid)) {
          throw new BadCertTemplateException(String.format("EC curve %s (OID: %s) is not allowed",
              AlgorithmUtil.getCurveName(curveOid), curveOid.getId()));
        }
      } else {
        throw new BadCertTemplateException("only namedCurve EC public key is supported");
      }

      // point encoding
      if (ecOption.pointEncodings() != null) {
        byte[] keyData = publicKey.getPublicKeyData().getBytes();
        if (keyData.length < 1) {
          throw new BadCertTemplateException("invalid publicKeyData");
        }
        byte pointEncoding = keyData[0];
        if (!ecOption.pointEncodings().contains(pointEncoding)) {
          throw new BadCertTemplateException(String.format(
              "not accepted EC point encoding '%s'", pointEncoding));
        }
      }

      byte[] keyData = publicKey.getPublicKeyData().getBytes();
      try {
        checkEcSubjectPublicKeyInfo(curveOid, keyData);
      } catch (BadCertTemplateException ex) {
        throw ex;
      } catch (Exception ex) {
        LogUtil.warn(LOG, ex, "checkEcSubjectPublicKeyInfo");
        throw new BadCertTemplateException(String.format(
            "invalid public key: %s", ex.getMessage()));
      }
      return publicKey;
    } else if (keyParamsOption instanceof RSAParametersOption) {
      RSAParametersOption rsaOption = (RSAParametersOption) keyParamsOption;

      ASN1Integer modulus;
      try {
        ASN1Sequence seq = ASN1Sequence.getInstance(publicKey.getPublicKeyData().getBytes());
        modulus = ASN1Integer.getInstance(seq.getObjectAt(0));
      } catch (IllegalArgumentException ex) {
        throw new BadCertTemplateException("invalid publicKeyData");
      }

      int modulusLength = modulus.getPositiveValue().bitLength();
      if ((rsaOption.allowsModulusLength(modulusLength))) {
        return publicKey;
      }
    } else if (keyParamsOption instanceof DSAParametersOption) {
      DSAParametersOption dsaOption = (DSAParametersOption) keyParamsOption;
      ASN1Encodable params = publicKey.getAlgorithm().getParameters();
      if (params == null) {
        throw new BadCertTemplateException("null Dss-Parms is not permitted");
      }

      int plength;
      int qlength;

      try {
        ASN1Sequence seq = ASN1Sequence.getInstance(params);
        ASN1Integer rsaP = ASN1Integer.getInstance(seq.getObjectAt(0));
        ASN1Integer rsaQ = ASN1Integer.getInstance(seq.getObjectAt(1));
        plength = rsaP.getPositiveValue().bitLength();
        qlength = rsaQ.getPositiveValue().bitLength();
      } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ex) {
        throw new BadCertTemplateException("illegal Dss-Parms");
      }

      boolean match = dsaOption.allowsPlength(plength);
      if (match) {
        match = dsaOption.allowsQlength(qlength);
      }

      if (match) {
        return publicKey;
      }
    } else {
      throw new IllegalStateException(String.format(
          "should not reach here, unknown KeyParametersOption %s", keyParamsOption));
    }

    throw new BadCertTemplateException("the given publicKey is not permitted");
  } // method checkPublicKey

  protected void verifySubjectDnOccurence(X500Name requestedSubject)
      throws BadCertTemplateException {
    Args.notNull(requestedSubject, "requestedSubject");

    SubjectControl occurences = getSubjectControl();
    if (occurences == null) {
      return;
    }

    ASN1ObjectIdentifier[] types = requestedSubject.getAttributeTypes();
    for (ASN1ObjectIdentifier type : types) {
      RdnControl occu = occurences.getControl(type);
      if (occu == null) {
        throw new BadCertTemplateException(String.format(
            "subject DN of type %s is not allowed", oidToDisplayName(type)));
      }

      RDN[] rdns = requestedSubject.getRDNs(type);
      if (rdns.length > occu.getMaxOccurs() || rdns.length < occu.getMinOccurs()) {
        throw new BadCertTemplateException(String.format(
            "occurrence of subject DN of type %s not within the allowed range. "
            + "%d is not within [%d, %d]", oidToDisplayName(type),  rdns.length,
            occu.getMinOccurs(), occu.getMaxOccurs()));
      }
    }

    for (ASN1ObjectIdentifier m : occurences.getTypes()) {
      RdnControl occurence = occurences.getControl(m);
      if (occurence.getMinOccurs() == 0) {
        continue;
      }

      boolean present = false;
      for (ASN1ObjectIdentifier type : types) {
        if (occurence.getType().equals(type)) {
          present = true;
          break;
        }
      }

      if (!present) {
        throw new BadCertTemplateException(String.format(
            "required subject DN of type %s is not present",
            oidToDisplayName(occurence.getType())));
      }
    }
  } // method verifySubjectDnOccurence

  protected RDN createSubjectRdn(String text, ASN1ObjectIdentifier type, RdnControl option,
      int index) throws BadCertTemplateException {
    ASN1Encodable rdnValue = createRdnValue(text, type, option, index);
    return (rdnValue == null) ? null : new RDN(type, rdnValue);
  }

  /**
   * Creates GeneralName.
   *
   * @param requestedName
   *          Requested name. Must not be {@code null}.
   * @param modes
   *          Modes to be considered. Must not be {@code null}.
   * @return the created GeneralName
   * @throws BadCertTemplateException
   *         If requestedName is invalid or contains entries which are not allowed in the modes.
   */
  public static GeneralName createGeneralName(GeneralName requestedName,
      Set<Certprofile.GeneralNameMode> modes)
      throws BadCertTemplateException {
    Args.notNull(requestedName, "requestedName");

    int tag = requestedName.getTagNo();
    Certprofile.GeneralNameMode mode = null;
    if (modes != null) {
      for (Certprofile.GeneralNameMode m : modes) {
        if (m.getTag().getTag() == tag) {
          mode = m;
          break;
        }
      }

      if (mode == null) {
        throw new BadCertTemplateException("generalName tag " + tag + " is not allowed");
      }
    }

    switch (tag) {
      case GeneralName.rfc822Name:
      case GeneralName.dNSName:
      case GeneralName.uniformResourceIdentifier:
      case GeneralName.iPAddress:
      case GeneralName.registeredID:
      case GeneralName.directoryName:
        return new GeneralName(tag, requestedName.getName());
      case GeneralName.otherName:
        ASN1Sequence reqSeq = ASN1Sequence.getInstance(requestedName.getName());
        int size = reqSeq.size();
        if (size != 2) {
          throw new BadCertTemplateException("invalid otherName sequence: size is not 2: " + size);
        }

        ASN1ObjectIdentifier type = ASN1ObjectIdentifier.getInstance(reqSeq.getObjectAt(0));
        if (mode != null && !mode.getAllowedTypes().contains(type)) {
          throw new BadCertTemplateException("otherName.type " + type.getId() + " is not allowed");
        }

        ASN1Encodable asn1 = reqSeq.getObjectAt(1);
        if (! (asn1 instanceof ASN1TaggedObject)) {
          throw new BadCertTemplateException("otherName.value is not tagged Object");
        }

        int tagNo = ASN1TaggedObject.getInstance(asn1).getTagNo();
        if (tagNo != 0) {
          throw new BadCertTemplateException("otherName.value does not have tag 0: " + tagNo);
        }

        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(type);
        vector.add(new DERTaggedObject(true, 0, ASN1TaggedObject.getInstance(asn1).getObject()));
        return new GeneralName(GeneralName.otherName, new DERSequence(vector));
      case GeneralName.ediPartyName:
        reqSeq = ASN1Sequence.getInstance(requestedName.getName());

        size = reqSeq.size();
        String nameAssigner = null;
        int idx = 0;
        if (size > 1) {
          DirectoryString ds = DirectoryString.getInstance(
              ASN1TaggedObject.getInstance(reqSeq.getObjectAt(idx++)).getObject());
          nameAssigner = ds.getString();
        }

        DirectoryString ds = DirectoryString.getInstance(
            ASN1TaggedObject.getInstance(reqSeq.getObjectAt(idx++)).getObject());
        String partyName = ds.getString();

        vector = new ASN1EncodableVector();
        if (nameAssigner != null) {
          vector.add(new DERTaggedObject(false, 0, new DirectoryString(nameAssigner)));
        }
        vector.add(new DERTaggedObject(false, 1, new DirectoryString(partyName)));
        return new GeneralName(GeneralName.ediPartyName, new DERSequence(vector));
      default:
        throw new IllegalStateException("should not reach here, unknown GeneralName tag " + tag);
    } // end switch (tag)
  } // method createGeneralName

  private static RDN createDateOfBirthRdn(ASN1ObjectIdentifier type, ASN1Encodable rdnValue)
      throws BadCertTemplateException {
    Args.notNull(type, "type");

    String text;
    ASN1Encodable newRdnValue = null;
    if (rdnValue instanceof ASN1GeneralizedTime) {
      text = ((ASN1GeneralizedTime) rdnValue).getTimeString();
      newRdnValue = rdnValue;
    } else if (rdnValue instanceof ASN1String && !(rdnValue instanceof DERUniversalString)) {
      text = ((ASN1String) rdnValue).getString();
    } else {
      throw new BadCertTemplateException("Value of RDN dateOfBirth has incorrect syntax");
    }

    if (!SubjectDnSpec.PATTERN_DATE_OF_BIRTH.matcher(text).matches()) {
      throw new BadCertTemplateException(
          "Value of RDN dateOfBirth does not have format YYYMMDD000000Z");
    }

    if (newRdnValue == null) {
      newRdnValue = new DERGeneralizedTime(text);
    }

    return new RDN(type, newRdnValue);
  }

  private static RDN createPostalAddressRdn(ASN1ObjectIdentifier type, ASN1Encodable rdnValue,
      RdnControl control, int index) throws BadCertTemplateException {
    Args.notNull(type, "type");

    if (!(rdnValue instanceof ASN1Sequence)) {
      throw new BadCertTemplateException("rdnValue of RDN postalAddress has incorrect syntax");
    }

    ASN1Sequence seq = (ASN1Sequence) rdnValue;
    final int size = seq.size();
    if (size < 1 || size > 6) {
      throw new BadCertTemplateException(
          "Sequence size of RDN postalAddress is not within [1, 6]: " + size);
    }

    ASN1EncodableVector vec = new ASN1EncodableVector();
    for (int i = 0; i < size; i++) {
      ASN1Encodable line = seq.getObjectAt(i);
      String text;
      if (line instanceof ASN1String && !(line instanceof DERUniversalString)) {
        text = ((ASN1String) line).getString();
      } else {
        throw new BadCertTemplateException(
          String.format("postalAddress[%d] has incorrect syntax", i));
      }

      ASN1Encodable asn1Line = createRdnValue(text, type, control, index);
      vec.add(asn1Line);
    }

    return new RDN(type, new DERSequence(vec));
  }

  private static RDN[] getRdns(RDN[] rdns, ASN1ObjectIdentifier type) {
    Args.notNull(rdns, "rdns");
    Args.notNull(type, "type");

    List<RDN> ret = new ArrayList<>(1);
    for (int i = 0; i < rdns.length; i++) {
      RDN rdn = rdns[i];
      if (rdn.getFirst().getType().equals(type)) {
        ret.add(rdn);
      }
    }

    return CollectionUtil.isEmpty(ret) ? null : ret.toArray(new RDN[0]);
  }

  private static ASN1Encodable createRdnValue(String text, ASN1ObjectIdentifier type,
      RdnControl option, int index) throws BadCertTemplateException {
    String tmpText = text.trim();

    StringType stringType = null;

    if (option != null) {
      stringType = option.getStringType();
      String prefix = option.getPrefix();
      String suffix = option.getSuffix();

      if (prefix != null || suffix != null) {
        String locTmpText = tmpText.toLowerCase();
        if (prefix != null && locTmpText.startsWith(prefix.toLowerCase())) {
          tmpText = tmpText.substring(prefix.length());
          locTmpText = tmpText.toLowerCase();
        }

        if (suffix != null && locTmpText.endsWith(suffix.toLowerCase())) {
          tmpText = tmpText.substring(0, tmpText.length() - suffix.length());
        }
      }

      Pattern pattern = option.getPattern();
      if (pattern != null && !pattern.matcher(tmpText).matches()) {
        throw new BadCertTemplateException(
          String.format("invalid subject %s '%s' against regex '%s'",
              ObjectIdentifiers.oidToDisplayName(type), tmpText, pattern.pattern()));
      }

      tmpText = StringUtil.concat((prefix != null ? prefix : ""), tmpText,
          (suffix != null ? suffix : ""));

      int len = tmpText.length();
      Range range = option.getStringLengthRange();
      Integer minLen = (range == null) ? null : range.getMin();

      if (minLen != null && len < minLen) {
        throw new BadCertTemplateException(
          String.format("subject %s '%s' is too short (length (%d) < minLen (%d))",
            ObjectIdentifiers.oidToDisplayName(type), tmpText, len, minLen));
      }

      Integer maxLen = (range == null) ? null : range.getMax();

      if (maxLen != null && len > maxLen) {
        throw new BadCertTemplateException(
            String.format("subject %s '%s' is too long (length (%d) > maxLen (%d))",
                ObjectIdentifiers.oidToDisplayName(type), tmpText, len, maxLen));
      }
    }

    if (stringType == null) {
      stringType = StringType.utf8String;
    }

    return stringType.createString(tmpText.trim());
  } // method createRdnValue

  private static String oidToDisplayName(ASN1ObjectIdentifier type) {
    return ObjectIdentifiers.oidToDisplayName(type);
  }

  private static void checkEcSubjectPublicKeyInfo(ASN1ObjectIdentifier curveOid, byte[] encoded)
      throws BadCertTemplateException {
    Args.notNull(curveOid, "curveOid");
    Args.notNull(encoded, "encoded");
    Args.positive(encoded.length, "encoded.length");

    Integer expectedLength = ecCurveFieldSizes.get(curveOid);
    if (expectedLength == null) {
      X9ECParameters ecP = ECUtil.getNamedCurveByOid(curveOid);
      ECCurve curve = ecP.getCurve();
      expectedLength = (curve.getFieldSize() + 7) / 8;
      ecCurveFieldSizes.put(curveOid, expectedLength);
    }

    switch (encoded[0]) {
      case 0x02: // compressed
      case 0x03: // compressed
        if (encoded.length != (expectedLength + 1)) {
          throw new BadCertTemplateException("incorrect length for compressed encoding");
        }
        break;
      case 0x04: // uncompressed
      case 0x06: // hybrid
      case 0x07: // hybrid
        if (encoded.length != (2 * expectedLength + 1)) {
          throw new BadCertTemplateException("incorrect length for uncompressed/hybrid encoding");
        }
        break;
      default:
        throw new BadCertTemplateException(
            String.format("invalid point encoding 0x%02x", encoded[0]));
    }
  } // method checkEcSubjectPublicKeyInfo

}
