/*
 * Copyright 2014 Martin W. Kirst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nitram509.jmacaroons;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.github.nitram509.jmacaroons.CryptoTools.generate_derived_key;
import static com.github.nitram509.jmacaroons.CryptoTools.macaroon_hmac;
import static com.github.nitram509.jmacaroons.MacaroonsConstants.MACAROON_MAX_CAVEATS;
import static com.github.nitram509.jmacaroons.MacaroonsConstants.MACAROON_MAX_STRLEN;
import static com.github.nitram509.jmacaroons.util.ArrayTools.appendToArray;

/**
 * Used to build Macaroons
 * <pre>
 * String location = "http://www.example.org";
 * String secretKey = "this is our super secret key; only we should know it";
 * String identifier = "we used our secret key";
 * Macaroon macaroon = MacaroonsBuilder.create(location, secretKey, identifier);
 * </pre>
 */
public class MacaroonsBuilder {

  private String location;
  private String secretKey;
  private String identifier;
  private String[] caveats = new String[0];

  /**
   * @param location   location
   * @param secretKey  secretKey
   * @param identifier identifier
   */
  public MacaroonsBuilder(String location, String secretKey, String identifier) {
    this.location = location;
    this.secretKey = secretKey;
    this.identifier = identifier;
  }

  /**
   * @param location   location
   * @param secretKey  secretKey
   * @param identifier identifier
   * @return {@link com.github.nitram509.jmacaroons.Macaroon}
   */
  public static Macaroon create(String location, String secretKey, String identifier) {
    return new MacaroonsBuilder(location, secretKey, identifier).getMacaroon();
  }

  /**
   * @param serializedMacaroon serializedMacaroon
   * @return {@link com.github.nitram509.jmacaroons.Macaroon}
   * @throws com.github.nitram509.jmacaroons.NotDeSerializableException when serialized macaroon is not valid base64, length is to short or contains invalid packet data
   */
  public static Macaroon deserialize(String serializedMacaroon) throws IllegalArgumentException {
    return MacaroonsDeSerializer.deserialize(serializedMacaroon);
  }

  /**
   * throws java.security.InvalidKeyException      (wrapped within a RuntimeException)
   * throws java.security.NoSuchAlgorithmException (wrapped within a RuntimeException)
   *
   * @return {@link com.github.nitram509.jmacaroons.Macaroon}
   */
  public Macaroon getMacaroon() {
    assert this.location.length() < MACAROON_MAX_STRLEN;
    assert this.identifier.length() < MACAROON_MAX_STRLEN;
    try {
      byte[] key = generate_derived_key(this.secretKey);
      byte[] hmac = macaroon_hmac(key, this.identifier);
      for (String caveat : this.caveats) {
        hmac = macaroon_hmac(hmac, caveat);
      }
      return new Macaroon(location, identifier, caveats, hmac);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param macaroon  macaroon
   * @param secretKey secretKey
   * @return {@link com.github.nitram509.jmacaroons.MacaroonsBuilder}
   */
  public static MacaroonsBuilder modify(Macaroon macaroon, String secretKey) {
    MacaroonsBuilder builder = new MacaroonsBuilder(macaroon.location, secretKey, macaroon.identifier);
    if (macaroon.caveats != null && macaroon.caveats.length > 0) {
      builder.caveats = macaroon.caveats;
    }
    return builder;
  }

  /**
   * @param caveat caveat
   * @return this {@link com.github.nitram509.jmacaroons.MacaroonsBuilder}
   */
  public MacaroonsBuilder add_first_party_caveat(String caveat) {
    if (caveat != null) {
      assert caveat.length() < MACAROON_MAX_STRLEN;
      if (this.caveats.length + 1 > MACAROON_MAX_CAVEATS) {
        throw new IllegalStateException("Too many caveats. There are max. " + MACAROON_MAX_CAVEATS + " caveats allowed.");
      }
      this.caveats = appendToArray(this.caveats, caveat);
    }
    return this;
  }

  /**
   * @param caveat caveat
   * @return this {@link com.github.nitram509.jmacaroons.MacaroonsBuilder}
   */
  // TODO: implement and make public
  private MacaroonsBuilder add_third_party_caveat(String caveat) {
    if (caveat != null) {
      throw new UnsupportedOperationException("not yet implemented");
    }
    return this;
  }

}
