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

## Tietokannan edistyneet ominaisuudet ja alustusskriptit

Projektin taustatietokanta (MariaDB) on suunniteltu hyödyntämään edistyneitä relaatiotietokantaominaisuuksia suorituskyvyn, tietoturvan ja datan eheyden varmistamiseksi. Alla on listattu käytetyt SQL-komentorakenteet, joilla nämä ominaisuudet on toteutettu:

* **Tietoturva (Käyttöoikeuksien rajaus):** Rajapinnalle on luotu oma erillinen käyttäjä (`api_backend`), jolla on vain DML-oikeudet (SELECT, INSERT, UPDATE, DELETE). Tämä estää taulujen vahingollisen tai vihamielisen poistamisen (DROP/ALTER) sovelluksen kautta.
* **Temporaaliominaisuudet (System Versioning):** Tuotetauluun (`products`) on lisätty `SYSTEM VERSIONING`, mikä mahdollistaa tuotetietojen (esim. hintamuutosten) historiallisen seurannan.
* **Näkymät (Views):** Tietokantaan on luotu näkymä `v_order_summary`, joka laskee automaattisesti tilauskohtaiset yhteenvedot (tuotteiden määrä ja kokonaissumma) ilman monimutkaisia JOIN-kyselyitä sovellustasolla.
* **Liipaisimet (Triggers):** Varastosaldon hallintaa on automatisoitu liipaisimella (`trg_decrease_stock`), joka vähentää automaattisesti tuotteen saldoa, kun tilaukseen lisätään uusi rivi.
* **Tallennetut proseduurit ja transaktiot:** Asiakkaan ja osoitteen luonti on yhdistetty yhdeksi atomiseksi operaatioksi tietokantatason proseduurilla (`sp_add_customer_with_address`), joka hyödyntää `START TRANSACTION` ja `COMMIT` -komentoja datan eheyden takaamiseksi.
* **Indeksointi:** Hakujen ja suodatusten nopeuttamiseksi on luotu indeksit usein kysyttyihin sarakkeisiin (asiakkaan sähköposti, tilauksen tila ja tuotteen kategoria).
* **Optimistinen lukitus:** Tuotetauluun on lisätty `version`-sarake samanaikaisten päivitysten (concurrency) turvalliseen hallintaan.

### Tietokannan SQL-skripti

```sql
-- Luodaan API-käyttäjä ja annetaan oikeudet vain DML-operaatioihin
CREATE USER IF NOT EXISTS 'api_backend'@'localhost' IDENTIFIED BY 'ApiSalasana2024!';
GRANT SELECT, INSERT, UPDATE, DELETE ON projekti.* TO 'api_backend'@'localhost';
FLUSH PRIVILEGES;

-- Lisätään tauluun system versioning -ominaisuus ja versiointi optimistista lukitusta varten
ALTER TABLE products ADD SYSTEM VERSIONING;
ALTER TABLE products ADD version BIGINT DEFAULT 0;

-- Näkymä tilausten yhteenvetoon
CREATE OR REPLACE VIEW v_order_summary AS
SELECT 
    o.id AS order_id,
    o.customer_id,
    o.order_date,
    o.status,
    COUNT(oi.product_id) AS total_items,
    SUM(oi.quantity * oi.unit_price) AS total_price
FROM orders o
LEFT JOIN orderitems oi ON o.id = oi.order_id
GROUP BY o.id;

-- Liipaisin varastosaldon automaattiseen vähentämiseen
DELIMITER //
CREATE OR REPLACE TRIGGER trg_decrease_stock
AFTER INSERT ON orderitems
FOR EACH ROW
BEGIN
    UPDATE products 
    SET stock_quantity = stock_quantity - NEW.quantity 
    WHERE id = NEW.product_id;
END;
//
DELIMITER ;

-- Tallennettu proseduuri asiakkaan ja osoitteen turvalliseen luontiin
DELIMITER //
CREATE OR REPLACE PROCEDURE sp_add_customer_with_address(
    IN p_first_name VARCHAR(50), 
    IN p_last_name VARCHAR(50), 
    IN p_email VARCHAR(100), 
    IN p_phone VARCHAR(20),
    IN p_street VARCHAR(100), 
    IN p_postal VARCHAR(20), 
    IN p_city VARCHAR(50), 
    IN p_country VARCHAR(50)
)
BEGIN
    DECLARE v_customer_id INT;
    
    -- Aloitetaan transaktio
    START TRANSACTION;
    
    -- 1. Lisätään asiakas
    INSERT INTO customers (first_name, last_name, email, phone) 
    VALUES (p_first_name, p_last_name, p_email, p_phone);
    
    -- Napataan juuri luodun asiakkaan ID talteen
    SET v_customer_id = LAST_INSERT_ID();
    
    -- 2. Lisätään osoite käyttäen uutta ID:tä
    INSERT INTO customeraddresses (customer_id, street_address, postal_code, city, country)
    VALUES (v_customer_id, p_street, p_postal, p_city, p_country);
    
    -- Vahvistetaan molemmat lisäykset
    COMMIT;
END;
//
DELIMITER ;

-- Indeksoinnit haku- ja suorituskykyoptimointia varten
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_products_category ON products(category_id);

---

## Käynnistysohjeet
1. Varmista, että MariaDB on käynnissä koneellasi.
2. Tarkista tietokannan yhdistämistiedot `src/main/resources/application.properties` -tiedostosta.
3. Käynnistä sovellus IDE:stä tai Mavenilla (`mvn spring-boot:run`).
4. Selaa rajapintoja Swagger UI:n kautta (ks. linkki yllä) tai Postmanilla. Tietokantataulut ja viiteavaimet (Foreign keys) luodaan automaattisesti käynnistyksen yhteydessä.