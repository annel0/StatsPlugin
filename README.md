# StatsPlugin  
**Сбор статистики игроков для Minecraft Paper**  

---

## Описание  
StatsPlugin собирает и сохраняет статистику игроков в вашем мире Minecraft. Поддерживает хранение данных в локальных файлах (YAML) или удаленной базе данных MariaDB.  

---

## Сбор статистики  
Плагин отслеживает следующие метрики:  
- **Время в игре** (в минутах)  
- **Убито мобов**  
- **Съедено предметов** (например, еды)  
- **Пройденное расстояние** (в блоках)  
- **Разрушенных блоков**  
- **Смертей**  
- **Созданных предметов** (крафт)  
- **Использованных предметов**  
- **Открытых сундуков**  
- **Сообщений в чате**  

---

## Требования  
- **Сервер**: Paper 1.21+  
- **Для базы данных**: MariaDB (если выбрано хранение в БД)  

---

## Установка  
1. **Скачайте** JAR-файл плагина и поместите его в папку `plugins/`.  
2. **Перезагрузите** сервер.  
3. Настройте `config.yml` (см. раздел ниже).  

---

## Настройка  
### 1. config.yml  
Пример конфигурации:  
```yaml  
storage:  
  type: database # или "file"  
  database:  
    host: localhost  
    port: 3306  
    name: minecraft_stats  
    user: root  
    password: ""  
```  

### 2. Для базы данных MariaDB:  
- Создайте базу данных и таблицу `player_stats` (плагин автоматически создаст таблицу при первом запуске).  
- Убедитесь, что MariaDB доступна по указанному адресу и порту.  

---

## Использование  
- **Сохранение статистики**:  
  - При выходе игрока данные сохраняются автоматически.  
  - Для принудительного сохранения всех данных используйте:  
    ```bash  
    /stats saveall  
    ```  
  *(Команда пока не реализована. Добавьте её в код, если нужно.)*  

---

## Технические детали  
### Хранение данных  
- **Файлы**: Данные хранятся в `plugins/StatsPlugin/stats/` в формате YAML.  
- **База данных**: Используется таблица `player_stats` с полями:  
  ```sql  
  CREATE TABLE player_stats (  
    uuid CHAR(36) PRIMARY KEY,  
    play_time INT,  
    mobs_killed INT,  
    ...  
  );  
  ```  

### Обработчики событий  
- **onPlayerJoin/Quit**: Трекинг времени игры.  
- **onEntityDeath**: Счетчик убитых мобов.  
- **onPlayerChat**: Счетчик сообщений.  
- **onPlayerMove**: Подсчет перемещения.  

---

## Важные замечания  
1. **Для разработчиков**:  
   - Добавьте зависимость `mariadb-java-client` в `build.gradle`, если используется БД:  
     ```groovy  
     implementation 'org.mariadb.jdbc:mariadb-java-client:3.3.0'  
     ```  
2. **Производительность**:  
   - Сохранение в БД происходит асинхронно через `CompletableFuture`.  
   - Для файлов — синхронное сохранение при выходе игрока.  

---

## Лицензия  
MIT License.  

---

## Автор  
[Annelo](https://gitverse.ru/annelo).  
Вопросы и предложения: annelo@bk.com  

--- 

**Важно**: Перед использованием на продакшене сервере проверьте логи на ошибки и настройте бэкапы данных.