# Example of adding a comment to SQL in Slick

For example:

```scala
val action = table.filter(_.column === value).labelledResult("this is my query")
```

The action will execute something like:

```sql
select * from table where column = 'value' /* this is my query */
```

Danger: Never provide user-supplied text to the label: it's a SQL injection route.
