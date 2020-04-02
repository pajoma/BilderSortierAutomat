The following commands are supported in the migration shell. 



### 
Returns the number of objects in the given path




### Configure
Sets a parameter, example 

```configure source <path to source>```

```configure help``` prints a list of all configurable options

```configure view``` prints current values

### Collect
```collect scan``` resets the CSV file, scans the directory and collects the detected items in the in memory database

```collect load``` loads content from CSV file and caches it into in-memory database

```collect u``` stores the directory content into a CSV file

 

### Analyze
Analyse the data before the migration. The results are stored in a CSV file and can be edited in Excel or Google Sheets. 

```analyze help``` shows all supported commands

```analyse duplicates``` fingerprints all objects

```analyse rename``` computes possible new name  

```analyse labels``` tries to come up with tags

```analyse groups``` tries to group objects by common properties

### Migrate
