# Verkkokaupan REST API - Taustapalvelu

Tämä projekti on Spring Boot- ja MariaDB-pohjainen REST API -taustapalvelu verkkokaupalle / tilausjärjestelmälle. Järjestelmä tarjoaa kattavat CRUD-rajapinnat asiakkaiden, tuotteiden, toimittajien ja tilausten hallintaan.

## Interaktiivinen API-Dokumentaatio (Swagger UI)

Projektissa on hyödynnetty OpenAPI/Swagger-standardia. Kun sovellus on käynnissä (oletusportissa 8081), löydät graafisen, interaktiivisen API-dokumentaation osoitteesta:

**[http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)**

Swaggerin kautta näet kaikki päätepisteet, voit kokeilla niitä reaaliajassa ("Try it out") ja nähdä tarkat tietorakenteet. Alla on kuitenkin ohjeistuksen mukainen kirjallinen erittely.

---

## API-dokumentaatiosta näkymätön osuus (Tietokanta ja Arkkitehtuuri)

Käyttöliittymän ja rajapintojen taustalla on toteutettu useita edistyneitä ominaisuuksia datan eheyden, suorituskyvyn ja turvallisuuden takaamiseksi:

1. **Transaktiot (Transactions):** Tilausprosessi (`OrderService`) on suojattu `@Transactional`-annotaatiolla. Kun tilaus luodaan, järjestelmä tarkistaa saldon ja vähentää sitä. Jos saldo ei riitä tai tapahtuu virhe, koko tietokantaoperaatio perutaan (rollback), jotta rahoja ei veloiteta ilman varastokatetta.
2. **Datan eheyden suojaus (Rekursion esto & DTO:t):** Spring Bootin ja relaatiotietokantojen yleinen ongelma (Infinite JSON Loop) on estetty arkkitehtuuritasolla. Järjestelmä käyttää Data Transfer Object (DTO) -mallia (esim. `OrderDTO`) ja Jacksonin sääntöjä (`@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`), jotta asiakastietoja voidaan syöttää turvallisesti ilman, että vastaus kaatuu päättymättömään silmukkaan.
3. **Periytyminen (Inheritance):** Koodin ja tietokannan toiston vähentämiseksi `Customer` ja `Supplier` perivät yhteisen `BasePerson`-luokan (`@MappedSuperclass`), johon on keskitetty ID:t ja yhteystiedot.
4. **Indeksointi (Indexing) ja Suorituskyky:** Tietokannan suorituskykyä on optimoitu indeksoimalla usein haettavat sarakkeet (esim. tuotenimet ja asiakkaiden sähköpostit). Lisäksi tiedonhaut on tehty tehokkaasti JPA:n automaattisilla taululiitoksilla (esim. `findByCustomerId`).
5. **Älykäs tiedonhaku (Katalogi):** Asiakkaan tuotekatalogi ei ole vain yksinkertainen `findAll()`, vaan se hyödyntää tietokantatason ehtoa (`findByStockQuantityGreaterThan`), joka palauttaa dynaamisesti vain ne tuotteet, joita on oikeasti varastossa.

---

## API-Päätepisteiden kuvaus (Endpoints)

Tässä on kuvaus keskeisistä rajapinnoista, niiden käyttötarkoituksesta sekä dataformaateista.

### Asiakkaat (Customers)

**1. Luo uusi asiakas**
* **Käyttötarkoitus:** Rekisteröi uuden asiakkaan tietokantaan.
* **Kutsu:** `POST /api/customers`
* **Request Format (JSON):** `{ "firstName": "Matti", "lastName": "Meikäläinen", "email": "matti@esimerkki.fi" }`
* **Vastauksen muoto:** Palauttaa luodun asiakkaan JSON-objektina (sisältäen tietokannan generoiman ID:n).

**2. Lisää osoite asiakkaalle**
* **Käyttötarkoitus:** Liittää uuden toimitusosoitteen olemassa olevaan asiakasprofiiliin (1-to-Many relaatio).
* **Kutsu:** `POST /api/customers/{id}/addresses`
* **Request Format:** `{ "streetAddress": "Katu 1", "city": "Helsinki", "postalCode": "00100", "country": "Suomi" }`
* **Vastauksen muoto:** `201 Created` ja luotu osoiteobjekti.

**3. Hae asiakkaan tilaushistoria**
* **Käyttötarkoitus:** Palauttaa listan kaikista tilauksista, jotka on kytketty tiettyyn asiakkaaseen.
* **Kutsu:** `GET /api/customers/{id}/orders`
* **Request:** Ei bodya.
* **Vastauksen muoto:** JSON-taulukko (Array) `OrderDTO`-objekteja.

### Tuotteet ja Katalogi (Products)

**4. Hae asiakaskatalogi**
* **Käyttötarkoitus:** Palauttaa ostettavissa olevat tuotteet (varastosaldo > 0).
* **Kutsu:** `GET /api/products/catalog`
* **Vastauksen muoto:** JSON-taulukko tuoteobjekteja.

**5. Lisää uusi tuote**
* **Käyttötarkoitus:** Ylläpitäjän työkalu tuotteiden lisäämiseen. Yhdistää tuotteen kategoriaan ja toimittajaan (Foreign Keys).
* **Kutsu:** `POST /api/products`
* **Request Format:** `{ "name": "Läppäri", "price": 1000.0, "stockQuantity": 10, "category": {"id": 1}, "supplier": {"id": 1} }`
* **Vastauksen muoto:** `201 Created` tallennettu tuote.

### Tilaukset (Orders) - Liiketoimintalogiikka

**6. Luo tilaus (Osto)**
* **Käyttötarkoitus:** Järjestelmän monimutkaisin päätepiste. Sitoo asiakkaan tuotteisiin, lukitsee ostohetken hinnan ja vähentää varastosaldoa transaktion sisällä.
* **Kutsu:** `POST /api/orders`
* **Request Format:**
  ```json
  {
    "customerId": 1,
    "status": "PENDING",
    "items": [
      { "product": { "id": 1 }, "quantity": 2 }
    ]
  }
  ```
* **Vastauksen muoto:** `201 Created` ja luotu tilausobjekti. Jos saldo ei riitä, palauttaa `400 Bad Request` virheilmoituksella.
* * **Vastauksen muoto:** JSON-objekti tilauksesta (OrderDTO).

**7. Hae tulostettava kuitti**
* **Käyttötarkoitus:** Palauttaa asiakkaalle muotoillun koosteen tilauksesta.
* **Kutsu:** `GET /api/orders/{id}/receipt`
* **Vastauksen muoto:** Map-rakenne, joka sisältää kuittinumeron, päivämäärän, tilarivikoosteen ja loppusumman.

---

## Käynnistysohjeet
1. Varmista, että MariaDB on käynnissä koneellasi.
2. Tarkista tietokannan yhdistämistiedot `src/main/resources/application.properties` -tiedostosta.
3. Käynnistä sovellus IDE:stä tai Mavenilla (`mvn spring-boot:run`).
4. Selaa rajapintoja Swagger UI:n kautta (ks. linkki yllä) tai Postmanilla. Tietokantataulut ja viiteavaimet (Foreign keys) luodaan automaattisesti käynnistyksen yhteydessä.