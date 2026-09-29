# Pruebas HTTP de Taco Cloud en el puerto 8080

Este procedimiento acompaña las fichas TC-01 a TC-36. La aplicación local publica los contratos nuevos bajo `/api/v1/**` y conserva rutas `/api/**` como alias de transición. El PDF usa principalmente las rutas sin versión; en las pruebas de esta documentación se recomienda `/api/v1/**`.

La [colección de Postman](TacoCloud.postman_collection.json) reúne los casos HTTP por reto. Ejecute primero **00 Preparación de datos** y después los folders TC-01 a TC-36 en orden. La colección usa datos sintéticos, guarda IDs de los recursos creados y renueva automáticamente CSRF y la cookie antes de cada operación de escritura. Ejecútela sobre una base de pruebas: crea órdenes, usuarios, pagos simulados, favoritos y calificaciones. Las comprobaciones de concurrencia, broker, outbox y DLQ que no son observables por HTTP siguen requiriendo las pruebas Java citadas en las fichas.

## Preparación en Postman

1. Cree una variable `baseUrl` con el valor `http://localhost:8080`.
2. Inmediatamente antes de cada petición de escritura, ejecute `GET {{baseUrl}}/csrf`. La respuesta incluye `headerName`, `parameterName` y `token`. Copie **el valor de `token`** a una variable `csrfToken`.
3. Mantenga habilitado el gestor de cookies de Postman. El servidor envía una cookie `JSESSIONID`; debe acompañar la petición que use ese token.
4. En cada `POST`, `PUT`, `PATCH` o `DELETE`, configure `Authorization` como **Basic Auth**, usuario `habuma` y contraseña `password`; agregue el header `X-CSRF-TOKEN: {{csrfToken}}`. Para cuerpos JSON, agregue `Content-Type: application/json` y `Accept: application/json`.
5. Para `GET`, consulte directamente la ruta requerida. Los recursos privados también requieren Basic Auth y el rol correspondiente. La cuenta de demostración puede recibir 403 en rutas exclusivas de `ADMIN` o `KITCHEN`.

El token puede renovarse entre peticiones. En la aplicación local, un segundo PUT con el token anterior devolvió 403; al repetir `GET /csrf` en la misma sesión respondió con el 400 esperado para el cuerpo inválido. Nunca copie un token fijo a las fichas.

## Ejemplo reproducible en PowerShell

El siguiente ejemplo consulta el token y prueba el manejo de un ingrediente ausente. El ID elegido no corresponde a un ingrediente del catálogo, de modo que la petición no altera datos existentes.

```powershell
$baseUrl = 'http://localhost:8080'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod -Uri "$baseUrl/csrf" -WebSession $session
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('habuma:password'))
$headers = @{
  'X-CSRF-TOKEN' = $csrf.token
  'Authorization' = "Basic $basic"
  'Accept' = 'application/json'
}
$body = '{"name":"Ingredient Test","type":"SAUCE"}'
Invoke-WebRequest -Uri "$baseUrl/api/v1/ingredients/NO_EXISTE_TC_DOC" `
  -Method Put -WebSession $session -Headers $headers `
  -ContentType 'application/json' -Body $body
```

La respuesta esperada es `404 Not Found`. PowerShell puede presentar los estados 4xx como excepción; inspeccione `Exception.Response.StatusCode` para confirmar el estado.

## Reglas de interpretación

Los códigos, cuerpos y encabezados de cada caso se comprueban contra el contrato vigente de la aplicación. Un `200` por sí solo no demuestra que una escritura ocurrió: confirme el cambio mediante un GET posterior. En las pruebas de concurrencia, inventario, eventos y outbox, complemente Postman con las pruebas automatizadas señaladas en cada ficha.

Las capturas de código de las fichas reproducen fragmentos del repositorio en el momento de generar esta documentación. Son una ayuda visual para localizar clases y métodos, no sustituyen la lectura del archivo completo ni prueban que todos los criterios pasen.
