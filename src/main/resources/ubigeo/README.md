# Catalogo UBIGEO local (INEI 2025)

`ubigeo-inei-2025.csv` es un catalogo local de departamentos, provincias y
distritos del Peru usado por la API para validar ubicaciones y alimentar los
selectores de UBIGEO. La API no consulta al INEI ni a ningun proveedor externo
en tiempo de ejecucion.

## Fuente oficial

- Entidad: Instituto Nacional de Estadistica e Informatica (INEI).
- Recurso: "Lista de Ubicacion Geografica" - Busqueda segun ubicacion geografica.
- Version: UBIGEO 2025.
- URL consultada:
  <https://webapp.inei.gob.pe:8443/sisconcode/ubigeo/listaBusquedaUbigeoPorUbicacionGeografica.htm?flagDist&flagDpto&flagProv&nivel=1&strDist=TODOS&strDpto=TODOS&strProv=TODOS&strVersion=2025&versionCategoriaPK=8-1>
- Fecha de consulta: 2026-10-19.
- Total importado: 25 departamentos, 196 provincias y 1892 distritos.

## Transformacion aplicada

Se extrajeron unicamente los codigos y nombres oficiales de la tabla de
ubicacion geografica. El archivo conserva la jerarquia
`departamento -> provincia -> distrito`; no incluye coordenadas, altitudes ni
otros datos del portal. Los nombres se decodificaron desde la codificacion
ISO-8859-1 del origen y se guardaron en UTF-8.

Columnas separadas por punto y coma:

```text
department_code;department_name;province_code;province_name;district_code;district_name
```

## Licencia y atribucion

Los datos son informacion publica oficial del Estado peruano publicada por el
INEI. Se reproducen solo codigos y nombres con fines de validacion y
visualizacion, atribuyendo la fuente al INEI. No se declara una licencia
distinta de los terminos de uso del portal del INEI; antes de redistribuir el
archivo fuera de este proyecto debe revisarse la vigencia de dichos terminos.

## Actualizacion

Si el INEI publica una nueva version, se debe reemplazar el CSV y actualizar
este README (version, URL y fecha de consulta). El cambio debe acompanarse de
pruebas de jerarquia y de la documentacion de API.
