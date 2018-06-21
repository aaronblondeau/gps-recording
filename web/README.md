

## Development

Running dev server

```
npm run serve
```

## Tests

### Running tests

```
npm run test:e2e
```

### Writing tests

Run development server and then launch cypress with baseUrl

```
export CYPRESS_baseUrl=http://localhost:8080
./node_modules/.bin/cypress open -c cypress.json
```

or in PowerShell

```
$env:CYPRESS_baseUrl = "http://localhost:8080"
./node_modules/.bin/cypress open -c cypress.json
```
