# Documentación de los retos funcionales de Taco Cloud

Las fichas TC-01 a TC-36 siguen la estructura de `TC-01_DocFinal.docx`. Describen el objetivo, la modificación solicitada, la prueba esperada y el criterio de aceptación de cada reto. Las rutas y archivos citados se contrastaron con este repositorio. Los casos HTTP son instrucciones de verificación; no equivalen por sí mismos a un resultado aprobado.

Antes de ejecutar peticiones que modifican datos, consulte [la guía de pruebas HTTP](PRUEBAS_HTTP.md) para obtener `X-CSRF-TOKEN` y mantener la cookie de sesión junto con la autenticación básica de demostración.

Importe [la colección de Postman](TacoCloud.postman_collection.json) para ejecutar las pruebas HTTP documentadas por reto.

| Laboratorio | Retos | Tema |
|---|---|---|
| 1 | [TC-01](TC-01.md), [TC-02](TC-02.md), [TC-03](TC-03.md), [TC-04](TC-04.md), [TC-05](TC-05.md), [TC-06](TC-06.md) | Cazar operaciones fantasma |
| 2 | [TC-07](TC-07.md), [TC-08](TC-08.md), [TC-09](TC-09.md), [TC-10](TC-10.md), [TC-11](TC-11.md), [TC-12](TC-12.md) | Contratos y seguridad |
| 3 | [TC-13](TC-13.md), [TC-14](TC-14.md), [TC-15](TC-15.md), [TC-16](TC-16.md), [TC-17](TC-17.md), [TC-18](TC-18.md) | Motor de negocio |
| 4 | [TC-19](TC-19.md), [TC-20](TC-20.md), [TC-21](TC-21.md), [TC-22](TC-22.md), [TC-23](TC-23.md), [TC-24](TC-24.md) | Funciones que sí dan ganas de usar |
| 5 | [TC-25](TC-25.md), [TC-26](TC-26.md), [TC-27](TC-27.md), [TC-28](TC-28.md), [TC-29](TC-29.md), [TC-30](TC-30.md) | Cocina y mensajería confiable |
| 6 | [TC-31](TC-31.md), [TC-32](TC-32.md), [TC-33](TC-33.md), [TC-34](TC-34.md), [TC-35](TC-35.md), [TC-36](TC-36.md) | Operación y calidad |

## Alcance de la evidencia

Cada imagen de código muestra un extracto de un archivo real del repositorio, con su ruta y numeración. Las líneas extensas se recortan en la imagen; el enlace lleva al archivo completo. El texto del PDF conserva el carácter de requisito. Se incluyen imágenes de respuestas HTTP sólo cuando la respuesta se verificó en la aplicación en ejecución.
