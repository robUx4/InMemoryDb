# InMemoryDb

Some helper classes to use Android database with storage on disk and in memory (optional).

You may be interested in [the Content Provider Generator][1] that can generate all the data source code based on a JSON
definition of the database fields. It can even generate a Content Provider or a `CursorLoader` for you.

## Principle

The asynchronous system consists of 2 parts:

* A `DataSource` that can be an SQLite database or a Content Provider.
* An `AsynchronousDbHelper` class that handles all the asynchronous calls.

All asynchronous read/write/query calls are done in a single thread for your app, even when having multiple databases.

### Data Sources

* `SqliteDataSource`: a basic SQLite database.
* `SqliteMapDataSource`: a SQLite database that reads/writes key/values instead of flat items.
* `ContentProviderDataSource`: a content-provider source based on a Uri to read/write the elements.

They all use a `DatabaseElementHandler` class that turns a `Cursor` into the item and an item into select queries for `update()` and `delete()` methods.

There are also typed variants (eg `TypedSqliteDataSource`) for stronger typing of the Cursor.

### Asynchronous Helper

The `AsynchronousDbHelper` is responsible for queuing the asynchronous read/write/queries for its data source. It's also responsible for turning an item into `ContentValues` when using `insert()` and `update()`.

There are plenty of helper classes for most use-case:

* `AsynchronousDatabase`: basic asynchronous helper with an interface similar to `List`, nothing is cached in memory.
* `InMemoryDbList`: cache elements into a `List` in memory and read/write elements in the background.
* `InMemoryDbArrayList`: similar to `InMemoryDbList` but read/write into the memory cache is thread-safe.
* `InMemoryDbCopyOnWriteArrayList`: similar to `InMemoryDbList` using a `CopyOnWriteArrayList` in-memory storage.
* `InMemoryDbSet`: cache elements into a `Set` in memory and read/write elements in the background.
* `InMemoryDbTreeSet`: similar to `InMemoryDbSet` using a `TreeSet` in-memory storage and thread-safe.
* `InMemoryDbMap`: cache elements into a `Map` in memory and read/write elements in the background.
* `InMemoryHashmapDb`: similar to `InMemoryDbMap` using a `Map` in-memory storage and thread-safe.
* `InMemoryLruCache`: cache elements in memory using a LRU cache and read/write elements in the background.

### AsyncDatabaseHandler

In addition to AsynchronousDbHelper there's another class that can be used to do asynchronous operations. It is similar to Android's `AsyncQueryHandler`. You pass an int token and a cookie object to each query and get a callback with these values when the operation completed. It's a convenient way to chain operation on the data source.

### Purge

There are helper classes to handle the database purge. The `PurgeHandler` is called everytime an element is added to the database. It's up to the handler to clean the data source as often as it wants.

* `DatabaseSourcePurger`: basic purge with a maximum number of items to keep in the data source.
* `DatabaseSourcePurgerMax`: purge with a maximum number of items to keep with sorting based on a field of the data source.
* `DatabasePurgerMaxDate`: purge with a maximum number of items to keep sorted by date.

### Adapters

In addition some `BaseAdapter` adapters are provided to read the data from the in-memory cache and to filter on the fly elements in-memory for the display.

## Download

Download [the latest JAR][2] or grab via Maven [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.gawst/asyncdb/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.gawst/asyncdb)
```xml
<dependency>
  <groupId>org.gawst</groupId>
  <artifactId>asyncdb</artifactId>
  <version>3.1.1</version>
</dependency>
```
or Gradle:
```groovy
compile 'org.gawst:asyncdb:3.1.1'
```


## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[1]: https://github.com/robUx4/android-contentprovider-generator
[2]: https://search.maven.org/remote_content?g=org.gawst&a=asyncdb&v=LATEST
