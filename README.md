# Cinema ticket reservation system

سامانهٔ Backend رزرو بلیط سینما با Java 25، Spring Boot، PostgreSQL و احراز هویت مبتنی بر Session.

## وضعیت پیاده‌سازی

ثبت‌نام، ورود و خروج با Session، فیلم‌ها و جستجوی عنوان، CRUD مدیر، رزرو و لغو بلیت، صندلی‌های آزاد و آمار فروش پیاده شده‌اند.
۲۵ تست با PostgreSQL واقعی شامل دو تست هم‌زمانی با Executor Framework اجرا شده‌اند.
خروجی اجرای ثبت‌شده در `docs/test-results.txt` قرار دارد. برای تحویل طبق PDF، از اجرای تست هم‌زمانی نیز اسکرین‌شات بگیرید.

## اجرا با Docker Compose

```bash
docker compose up --build -d
docker compose logs -f app
```

برنامه روی `http://localhost:8080` در دسترس است. صفحهٔ وب برای `/` تعریف نشده؛ APIها را با Postman آزمایش کنید.

## اجرا بدون کانتینر برنامه

JDK 25 و Maven لازم‌اند. PostgreSQL را اجرا و دیتابیس `cinema` را ایجاد کنید. می‌توانید فقط PostgreSQL را با Compose اجرا کنید:

```bash
docker compose up -d postgres
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cinema
export SPRING_DATASOURCE_USERNAME=cinema_user
export SPRING_DATASOURCE_PASSWORD=cinema_pass
mvn spring-boot:run
```

دستور `java_home` مخصوص macOS است؛ در سیستم‌های دیگر `JAVA_HOME` را به مسیر JDK 25 تنظیم کنید.
اگر نسخهٔ Docker برنامه روی پورت 8080 روشن است، ابتدا `docker compose stop app` را اجرا کنید.
متغیرهای اتصال داخل سرویس برنامه از نام میزبان `postgres` و هنگام اجرای محلی از `localhost` استفاده می‌کنند.

## داده‌های اولیه

`DataLoader` هنگام شروع برنامه، موارد گمشده را اضافه می‌کند و دادهٔ موجود را بازنویسی نمی‌کند:

| نام کاربری | رمز نمونه | نقش |
|---|---|---|
| admin | admin123 | ADMIN |
| user1 | user123 | USER |

رمزها با BCrypt ذخیره می‌شوند. سالن «سالن شماره 1» با 50 صندلی و سه فیلم نمونه نیز ساخته می‌شوند.
اگر قبلاً نام کاربری یا ایمیل نمونه را ثبت کرده‌اید، Loader آن حساب را تغییر نمی‌دهد؛ رمز و نقش همان مقدار قبلی می‌مانند.
حساب‌های نمونه صرفاً برای پروژهٔ آموزشی‌اند.

## APIهای موجود

| متد | مسیر | دسترسی | موفقیت |
|---|---|---|---|
| POST | /api/auth/register | عمومی | 201 |
| POST | /api/auth/login | عمومی | 200 |
| GET | /api/auth/me | کاربر واردشده | 200 |
| POST | /api/auth/logout | کاربر واردشده | 200 |
| GET | /api/auth/csrf | عمومی | 200 |
| GET | /api/movies | عمومی | 200 |
| GET | /api/movies/{id} | عمومی | 200 |
| GET | /api/movies/search?title=cept | عمومی | 200 |
| POST | /api/admin/movies | ADMIN + CSRF | 201 |
| PUT | /api/admin/movies/{id} | ADMIN + CSRF | 200 |
| DELETE | /api/admin/movies/{id} | ADMIN + CSRF | 204 |
| GET | /api/movies/{id}/available-seats | عمومی | 200 |
| POST | /api/tickets/book | USER + CSRF | 201 |
| GET | /api/tickets/my-tickets | USER | 200 |
| DELETE | /api/tickets/{id} | USER + CSRF | 204 |
| GET | /api/admin/tickets/stats | ADMIN | 200 |

پاسخ موفق دارای `message` و `data` است؛ حذف موفق بدنه ندارد. خطاها شامل `timestamp`، `status`، `error`، `message` و `path` هستند.
ورود نامعتبر یا دسترسی بدون ورود `401`، نقش نامناسب یا CSRF نامعتبر `403`، دادهٔ پیدا‌نشده `404` و ورودی نامعتبر `400` می‌دهد.

### ثبت‌نام و ورود

ثبت‌نام با `Content-Type: application/json`:

```json
{"username":"student1","email":"student1@example.com","password":"student123"}
```

برای ورود ادمین:

```json
{"username":"admin","password":"admin123"}
```

Cookie مربوط به Session را نگه دارید. ثبت‌نام به‌تنهایی ورود انجام نمی‌دهد.

### افزودن و ویرایش فیلم

بعد از Login، با همان Cookie درخواست `GET /api/auth/csrf` بفرستید. مقدار `data.token` را با نام Header موجود در `data.headerName` در درخواست‌های POST/PUT/DELETE فیلم ارسال کنید.

بدنهٔ نمونه (شناسهٔ واقعی سالن را وارد کنید؛ در پاسخ فیلم‌های نمونه موجود است):

```json
{
  "title": "New Movie",
  "genre": "Drama",
  "duration": 120,
  "releaseDate": "2026-10-29",
  "description": "A sample movie.",
  "cinemaHallId": 1
}
```

نمونهٔ پاسخ `201`؛ شناسه‌ها نمونه‌اند:

```json
{
  "message": "فیلم ایجاد شد",
  "data": {
    "id": 4,
    "title": "New Movie",
    "genre": "Drama",
    "duration": 120,
    "releaseDate": "2026-10-29",
    "description": "A sample movie.",
    "cinemaHallId": 1,
    "cinemaHallName": "سالن شماره 1"
  }
}
```

جستجو تطابق نسبی عنوان را بدون حساسیت به بزرگی حروف انجام می‌دهد؛ نتیجهٔ جستجوی ناموجود لیست خالی است.
لیست‌ها براساس عنوان و سپس شناسه مرتب‌اند. جستجوی عنوان خالی همهٔ فیلم‌ها را برمی‌گرداند.
در این مدل ساده هر فیلم یک سالن دارد؛ موجودیت سانس هنوز تعریف نشده است.

### رزرو، مشاهده و لغو بلیت

با `user1` وارد شوید، Cookie مربوط به Session را نگه دارید و از `/api/auth/csrf` توکن بگیرید.
درخواست‌های رزرو و لغو، مانند عملیات تغییر فیلم، به Header مربوط به CSRF نیاز دارند.
اطلاعات خریدار از کاربر واردشده گرفته می‌شود؛ بدنهٔ درخواست نباید شناسهٔ خریدار یا قیمت را تعیین کند.

`POST /api/tickets/book`:

```json
{"movieId":1,"seatNumber":12}
```

نمونهٔ پاسخ `201` (شناسه و زمان نمونه‌اند):

```json
{
  "message": "بلیت رزرو شد",
  "data": {
    "id": 1,
    "movieId": 1,
    "movieTitle": "Inception",
    "cinemaHallId": 1,
    "cinemaHallName": "سالن شماره 1",
    "seatNumber": 12,
    "purchasedAt": "2026-10-29T22:00:00",
    "price": 150000.00
  }
}
```

`GET /api/tickets/my-tickets` فقط بلیت‌های همان کاربر را، از جدید به قدیم، داخل `data` برمی‌گرداند.
`DELETE /api/tickets/1` بلیت خود کاربر را حذف می‌کند، پاسخ `204` دارد و صندلی را دوباره آزاد می‌کند.
لغو بلیت کاربر دیگر `403` و شناسهٔ ناموجود `404` می‌دهد.
در این نسخه لغو به معنی حذف بلیت است؛ تاریخچهٔ لغو و عملیات بانکی در صورت پروژه تعریف نشده‌اند.

`GET /api/movies/1/available-seats` شماره‌های آزاد را مرتب، داخل `data` برمی‌گرداند، مثلاً:

```json
{"message":"صندلی‌های آزاد","data":[1,2,3,4,5]}
```

این لیست یک تصویر از وضعیت لحظهٔ خواندن است؛ آزادبودن در لیست، رزرو را تضمین نمی‌کند و هنگام خرید دوباره بررسی می‌شود.
شماره‌گذاری از **۱ تا خود ظرفیت سالن** است. برای سالن ۵۰ نفره، صندلی ۵۰ معتبر و ۵۱ نامعتبر است.
PDF عبارت «کمتر از ظرفیت» دارد؛ این نسخه آن را به بازهٔ ۱ تا ظرفیت تفسیر کرده تا همهٔ ۵۰ صندلی قابل رزرو باشند.
صفر، عدد منفی و عدد بزرگ‌تر از ظرفیت `400`، صندلی فروخته‌شده `409` می‌دهد.
نقش ADMIN به‌تنهایی دسترسی USER ندارد؛ مسیرهای بلیت مطابق PDF نقش USER می‌خواهند.

قیمت با `BigDecimal` ذخیره می‌شود و پیش‌فرض آن **۱۵۰٬۰۰۰ تومان** است؛ با متغیر `CINEMA_TICKET_PRICE` یا ویژگی `cinema.ticket-price` قابل تنظیم است.
در Compose می‌توانید `CINEMA_TICKET_PRICE` را به `environment` سرویس `app` اضافه کنید.
قیمت در زمان خرید روی بلیت ذخیره می‌شود؛ تغییر تنظیمات، قیمت بلیت قبلی را عوض نمی‌کند.

### آمار مدیر

`GET /api/admin/tickets/stats` با Session ادمین:

```json
{"message":"آمار بلیت‌های فعال","data":{"totalTickets":2,"totalRevenue":300000.00}}
```

آمار فقط بلیت‌های لغونشده را حساب می‌کند. مجموع درآمد از قیمت ذخیره‌شدهٔ بلیت‌ها محاسبه می‌شود.
بدون بلیت، هر دو مقدار صفرند. مجموع و تعداد با یک Query محاسبه می‌شوند.
حذف فیلم یا تغییر سالنِ فیلمِ دارای بلیت فعال با `409` رد می‌شود؛ پس از لغو همهٔ بلیت‌های آن فیلم این عملیات مجاز است.

### جلوگیری از فروش هم‌زمان یک صندلی

روی `Movie` فیلد `@Version` داریم. هر رزرو یا لغو، `reservationRevision` فیلم را تغییر می‌دهد تا Hibernate هنگام ذخیره، نسخهٔ قبلی را بررسی و نسخه را افزایش دهد.
ذخیره/حذف بلیت و تغییر فیلم در **همان تراکنش** انجام می‌شوند؛ برخورد نسخه باعث rollback کل عملیات می‌شود.
قرار دادن `@Version` فقط روی بلیت جدید کافی نیست، چون دو درخواست می‌توانند دو ردیف جدید جداگانه بسازند.
همچنین محدودیت دیتابیس `uk_ticket_movie_seat` اجازهٔ وجود دو بلیت با یک `movie_id` و `seat_number` را نمی‌دهد.

قفل این نسخه در سطح فیلم است؛ دو رزرو هم‌زمان برای صندلی‌های متفاوت همان فیلم نیز ممکن است با `409` مواجه شوند.
در این حالت کلاینت باید صندلی‌ها را دوباره بگیرد و در صورت آزادبودن صندلی، دوباره درخواست دهد.
برای ترافیک بیشتر می‌توان موجودیت صندلی مستقل و نسخهٔ جداگانه برای هر صندلی تعریف کرد.

## Postman

فایل `postman/cinema-movies.postman_collection.json` را برای فیلم‌ها و `postman/cinema-tickets.postman_collection.json` را برای بلیت‌ها Import کنید و درخواست‌ها را به‌ترتیب اجرا کنید.
مجموعه، شناسهٔ سالن، CSRF token و شناسهٔ فیلم جدید را خودکار در متغیرهای Collection قرار می‌دهد.
مدیریت Cookieهای Postman باید فعال باشد. قبل از تست ورود، Cookieهای باقی‌مانده از حساب‌های دیگر را پاک کنید.
مجموعهٔ بلیت، صندلی آزاد انتخاب می‌کند، رزرو تکراری را بررسی می‌کند، بلیت را لغو و دوباره رزرو می‌کند و در پایان بلیت‌های آزمایشی خودش را حذف می‌کند.
مجموعه‌ها از نظر JSON و سینتکس اسکریپت‌ها بررسی شده‌اند؛ اجرای خود Postman جدا از تست‌های MockMvc است.

## تست‌ها

تست‌ها با PostgreSQL جداگانه روی پورت 55432 اجرا می‌شوند؛ به دیتابیس اصلی پروژه متصل نمی‌شوند:

```bash
docker run --rm -d --name cinema-ticket-tests \
  -e POSTGRES_DB=cinema_test -e POSTGRES_USER=cinema_test \
  -e POSTGRES_PASSWORD=cinema_test \
  -p 127.0.0.1:55432:5432 postgres:17-alpine
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
mvn test
docker stop cinema-ticket-tests
```

قبل از اجرای تست، آماده‌شدن دیتابیس را با `docker exec cinema-ticket-tests pg_isready -U cinema_test -d cinema_test` بررسی کنید.
پروفایل `test` از `create-drop` استفاده می‌کند؛ متغیرهای `TEST_DATABASE_URL`، `TEST_DATABASE_USERNAME` و `TEST_DATABASE_PASSWORD` را فقط برای دیتابیس قابل حذف تنظیم کنید.
تست‌ها Repositoryهای واقعی، CRUD فیلم، جستجو، نقش‌ها، CSRF، اعتبارسنجی، idempotency داده‌های اولیه، ورود/خروج با Session، رزرو/لغو، مالکیت، قیمت و آمار را بررسی می‌کنند.
تست‌های هم‌زمانی در `TicketConcurrencyTests` بدون تراکنش بیرونی تست اجرا می‌شوند تا commit و rollback واقعی باشند:

1. دو خریدار با دو thread برای صندلی یکسان درخواست می‌دهند: یک پاسخ `201`، یک پاسخ `409` و فقط یک بلیت ذخیره می‌شود.
2. دو تراکنش با `CyclicBarrier` نسخهٔ یکسان فیلم را می‌خوانند و صندلی‌های متفاوت می‌خواهند: یک commit و یک rollback رخ می‌دهد؛ این تست عملکرد خود `@Version` را مستقل از یکتایی صندلی ثابت می‌کند.

برای اجرای فقط این تست‌ها و گرفتن اسکرین‌شات خروجی:

```bash
mvn -Dtest=TicketConcurrencyTests test
```

نتیجهٔ اجرای ثبت‌شده: **۲۵ تست، صفر خطا، صفر شکست، صفر تست ردشده**.

## Functional Programming

در `MovieService`، رابط استاندارد `Predicate<Movie>` برای فیلتر عنوان استفاده شده تا شرط فیلتر از تبدیل DTO جدا بماند.
`Comparator<Movie>` با Lambda عنوان‌ها را مرتب می‌کند و `thenComparing(Movie::getId)` ترتیب عنوان‌های یکسان را مشخص می‌کند.
Stream API و Method Reference در فیلتر، مرتب‌سازی و تبدیل فیلم‌ها به DTO استفاده شده‌اند.
جستجوی فعلی برای حجم کوچک پروژه داخل حافظه انجام می‌شود؛ برای دادهٔ زیاد باید جستجو و صفحه‌بندی به دیتابیس منتقل شود.
رابط اختصاصی `BookingRule` با `@FunctionalInterface` یک متد `validate(Movie, int)` دارد.
در سازندهٔ `TicketService` دو پیاده‌سازی Lambda برای بازهٔ صندلی و فروخته‌نشدن آن تعریف شده‌اند؛ `forEach` آن‌ها را قبل از ذخیره اجرا می‌کند.
این رابط، قرارداد قوانین رزرو را مشخص می‌کند و افزودن قانون تازه را بدون تغییر جریان اصلی رزرو ممکن می‌کند.
در لیست بلیت‌ها Method Reference برای تبدیل DTO و در صندلی‌های آزاد `IntStream` همراه با فیلتر و `HashSet` استفاده شده است.

## موارد باقی‌ماندهٔ تحویل

اسکرین‌شات اجرای تست هم‌زمانی را تهیه کنید، نسخهٔ جدید برنامهٔ Docker را با `docker compose up --build -d app` بسازید و مجموعه‌های Postman را اجرا کنید.
ساخت مخزن خصوصی GitHub، دادن دسترسی استاد و ارسال آدرس مخزن طبق PDF باید هنگام تحویل انجام شود.
