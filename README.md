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

Tässä on kattava luettelo järjestelmän kaikista rajapinnoista, jaoteltuna resurssien mukaan. Jokainen rajapinta palauttaa ja vastaanottaa dataa JSON-muodossa.

### 1. Asiakkaat ja Osoitteet (Customers & Addresses)

**Asiakkaiden perushallinta (`/api/customers`)**
* `GET /api/customers` - Palauttaa listan kaikista asiakkaista.
* `POST /api/customers` - Luo uuden asiakkaan.
  * *Request Format:* `{ "firstName": "Matti", "lastName": "Meikäläinen", "email": "matti@esimerkki.fi" }`
* `GET /api/customers/{id}` - Palauttaa yksittäisen asiakkaan tiedot ID:n perusteella.
* `PUT /api/customers/{id}` - Päivittää asiakkaan tiedot. *Request Format:* Sama kuin POST-metodissa.
* `DELETE /api/customers/{id}` - Poistaa asiakkaan tietokannasta.

**Asiakkaan osoitteiden hallinta**
* `GET /api/customers/{id}/addresses` - Palauttaa tietyn asiakkaan kaikki liitetyt osoitteet.
* `POST /api/customers/{id}/addresses` - Liittää uuden toimitusosoitteen asiakkaalle.
  * *Request Format:* `{ "streetAddress": "Katu 1", "city": "Helsinki", "postalCode": "00100", "country": "Suomi" }`

**Erillinen osoitehallinta (`/api/customer-addresses`)**
* `GET /api/customer-addresses` - Palauttaa kaikki järjestelmän osoitteet.
* `POST /api/customer-addresses` - Luo uuden irrallisen osoitteen.
* `GET /api/customer-addresses/{id}` - Palauttaa yksittäisen osoitteen tiedot.
* `PUT /api/customer-addresses/{id}` - Päivittää osoitteen tiedot.
* `DELETE /api/customer-addresses/{id}` - Poistaa osoitteen.

### 2. Tuotteet ja Kategoriat (Products & Categories)

**Kategorioiden hallinta (`/api/categories`)**
* `GET /api/categories` - Hakee kaikki tuotekategoriat.
* `POST /api/categories` - Luo uuden kategorian.
  * *Request Format:* `{ "name": "Elektroniikka" }`
* `GET /api/categories/{id}` - Hakee tietyn kategorian tiedot.
* `PUT /api/categories/{id}` - Päivittää kategorian nimen.
* `DELETE /api/categories/{id}` - Poistaa kategorian.
* `GET /api/categories/{id}/products` - Palauttaa kaikki tiettyyn kategoriaan kuuluvat tuotteet.

**Tuotteiden hallinta (`/api/products`)**
* `GET /api/products` - Palauttaa listan kaikista tuotteista.
* `POST /api/products` - Lisää uuden tuotteen tietokantaan.
  * *Request Format:* `{ "name": "Läppäri", "price": 1000.0, "stockQuantity": 10, "category": {"id": 1}, "supplier": {"id": 1} }`
* `GET /api/products/catalog` - Palauttaa ostettavissa olevat tuotteet (varastosaldo > 0). Tämä on asiakasnäkymään tarkoitettu suodatettu haku.
* `GET /api/products/search` - Etsii tuotteita dynaamisesti hakuparametrien perusteella.
* `PUT /api/products/{id}` - Päivittää tuotteen tiedot.
* `DELETE /api/products/{id}` - Poistaa tuotteen.
* `PUT /api/products/category/{categoryId}/raise-prices` - Massa-päivittää tietyn kategorian kaikkien tuotteiden hintoja (esim. prosentin mukainen korotus).

### 3. Toimittajat (Suppliers)

**Toimittajien hallinta (`/api/suppliers`)**
* `GET /api/suppliers` - Listaa kaikki järjestelmän toimittajat.
* `POST /api/suppliers` - Luo uuden toimittajan.
  * *Request Format:* `{ "name": "Tech Corp", "email": "contact@techcorp.com", "phone": "0501234567" }`
* `GET /api/suppliers/{id}` - Hakee yksittäisen toimittajan tiedot.
* `PUT /api/suppliers/{id}` - Päivittää toimittajan tiedot.
* `DELETE /api/suppliers/{id}` - Poistaa toimittajan.

### 4. Tilaukset (Orders) - Järjestelmän ydin

**Tilausten hallinta (`/api/orders`)**
* `GET /api/orders` - Palauttaa listan kaikista tilauksista.
* `POST /api/orders` - Luo uuden tilauksen. Tämä on järjestelmän monimutkaisin päätepiste: se lukitsee ostohetken hinnan ja vähentää varastosaldoa transaktion sisällä.
  * *Request Format:*
    ```json
    {
      "customerId": 1,
      "status": "PENDING",
      "items": [
        { "product": { "id": 1 }, "quantity": 2 }
      ]
    }
    ```
  * *Vastauksen muoto:* JSON-objekti tilauksesta (OrderDTO).
* `GET /api/orders/{id}` - Palauttaa tietyn tilauksen tarkan rakenteen ja tilarivit.
* `PATCH /api/orders/{id}/status` - Päivittää tilauksen tilan (esim. PENDING -> SHIPPED).
* `GET /api/orders/{id}/total` - Laskee ja palauttaa tilauksen tarkan kokonaissumman ostohetken hintojen perusteella.
* `GET /api/orders/{id}/receipt` - Palauttaa asiakkaalle muotoillun koosteen tilauksesta.
  * *Vastauksen muoto:* Map-rakenne, joka sisältää kuittinumeron, päivämäärän, tilarivikoosteen ja loppusumman.
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