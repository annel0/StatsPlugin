# StatsPlugin 📊  [![.github/workflows/gradle.yml](https://github.com/annel0/StatsPlugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/annel0/StatsPlugin/actions/workflows/gradle.yml)
**Адвинутый сбор статистики игроков для Minecraft Paper**  

---

## Описание  
StatsPlugin — это мощный плагин для сбора и анализа статистики игроков в вашем мире Minecraft. Он поддерживает хранение данных как в локальных файлах (YAML), так и в удаленной базе данных MariaDB, обеспечивая гибкость и надежность.  

---

## 📊 Отслеживаемые метрики  
Плагин собирает следующие данные:  
| Метрика                | Описание                              |  
|------------------------|---------------------------------------|  
| **Время в игре**       | Время, проведенное игроком (в минутах)|  
| **Убито мобов**        | Количество убитых мобов               |  
| **Съедено предметов**  | Количество съеденной еды              |  
| **Пройденное расстояние** | Дистанция перемещения (в блоках)    |  
| **Разрушенных блоков** | Количество сломанных блоков           |  
| **Смертей**            | Количество смертей игрока             |  
| **Созданных предметов**| Количество скрафченных предметов      |  
| **Использованных предметов** | Количество использованных предметов |  
| **Открытых сундуков**  | Количество открытых сундуков          |  
| **Сообщений в чате**   | Количество отправленных сообщений     |  

---

## 🚀 Требования  
- **Сервер**: Paper 1.21+  
- **Для базы данных**: MariaDB (если выбрано хранение в БД)  
- **Java**: 17+  

---

## 🛠 Установка  
1. **Скачайте** последнюю версию плагина с [GitVerse](https://gitverse.ru/annelo/PluginTamplate).  
2. Переместите JAR-файл в папку `plugins/` вашего сервера.  
3. Перезапустите сервер.  
4. Настройте `config.yml` (подробности ниже).  

---

## 🛠 Настройка  
### 1. Конфигурация `config.yml`  
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
logging:  
  level: INFO # Уровень логирования (DEBUG/INFO/WARN/ERROR)  
```  

### 2. Настройка MariaDB  
- Создайте базу данных и таблицу `player_stats` (плагин автоматически создаст таблицу при первом запуске).  
- Убедитесь, что MariaDB доступна по указанному адресу и порту.  

---

## 🔧 Использование  
- **Автоматическое сохранение**:  
  - Данные сохраняются при выходе игрока.  
  - Для принудительного сохранения всех данных используйте команду:  
    ```bash  
    /stats saveall  
    ```  
  *(TODO: реализовать команду)*  

- **Просмотр статистики**:  
  - В разработке: планируется добавить команду `/stats [игрок]` для вывода данных в чат.  

---

## 🛠 Технические детали  
### Хранение данных  
| Метод хранения | Особенности                                                                 |  
|----------------|-----------------------------------------------------------------------------|  
| **Файлы (YAML)** | Данные хранятся в `plugins/StatsPlugin/stats/`. Рекомендуется для небольших серверов. |  
| **База данных** | Данные хранятся в таблице `player_stats` с оптимизированным SQL-запросом. |  

### Обработчики событий  
- **onPlayerJoin/Quit**: Точное отслеживание времени игры.  
- **onEntityDeath**: Счетчик убитых мобов с асинхронным сохранением.  
- **onPlayerMove**: Подсчет перемещения с точностью до блока.  
- **onPlayerChat**: Счетчик сообщений в реальном времени.  

---

## ⚠️ Важно  
1. **Для разработчиков**:  
   - Добавьте зависимость в `build.gradle` для работы с MariaDB:  
     ```groovy  
     implementation 'org.mariadb.jdbc:mariadb-java-client:3.3.0'  
     ```  
2. **Производительность**:  
   - База данных: асинхронное сохранение через `CompletableFuture`.  
   - Файлы: синхронное сохранение при выходе игрока.  

---

## 📝 Лицензия  
Этот плагин распространяется под лицензией **MIT**.  

---

## 📢 Автор  
**Annelo**  
- **GitHub**: [GitVerse Профиль](https://gitverse.ru/annelo)  
- **Контакты**: annelo@bk.com  

---

## 🚀 Визуализация  
![Статистика](https://example.com/statistics-screenshot.png) *Пример визуализации статистики (TODO: добавить скриншот)*  

---

**Важно**: Перед использованием на продакшене сервере проверьте логи на ошибки и настройте бэкапы данных.  

---  
*StatsPlugin — инструмент для тех, кто ценит аналитику и надежность!* 🚀
