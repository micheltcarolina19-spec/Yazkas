# 🐾 Yazka's · Peluquería canina y felina

Sistema web de gestión para **Yazka's**, una peluquería canina y felina de **Chiquinquirá, Boyacá (Colombia)**.
Incluye una landing pública, un **panel de cliente** para gestionar mascotas y citas, y un **panel de administración** para el negocio: agenda, bitácoras de servicio, inventario, ventas, gastos, finanzas y planilla de sanidad.

Construido con **Quarkus** siguiendo **arquitectura hexagonal (puertos y adaptadores)**.

---

## 📑 Tabla de contenido

- [Funcionalidades](#-funcionalidades)
- [Stack tecnológico](#-stack-tecnológico)
- [Arquitectura hexagonal](#-arquitectura-hexagonal)
- [Remoción de fondo con U²-Net](#-remoción-de-fondo-de-imágenes-con-u²-net)
- [Seguridad y roles](#-seguridad-y-validación-por-rol)
- [Generación de PDFs](#-generación-de-pdfs)
- [Estimación de duración de citas con IA](#-estimación-de-duración-de-citas-con-ia)
- [Requisitos](#-requisitos)
- [Configuración](#-configuración)
- [Ejecución](#-ejecución)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Reglas de negocio](#-reglas-de-negocio)

---

## ✨ Funcionalidades

### 🌐 Landing pública
- Inicio, **Servicios**, **Nosotros**, **Contacto** y **Catálogo** de productos (sin precios visibles para el cliente).
- Registro e inicio de sesión de clientes.

### 👤 Panel del cliente
- Registro y gestión de **mascotas** con un asistente por pasos (tipo, raza, sexo, pelaje, nivel de energía, nivel de agresividad y foto).
- **Perfil de mascota** con edad calculada, vacunaciones y desparasitaciones.
- **Agendamiento de citas** según disponibilidad real de la agenda, con opción de **reprogramar**.
- **Historial de citas** por mascota.
- **Descarga en PDF de la bitácora del último servicio**, incluyendo las fotos de salida de la mascota.

### 🛠️ Panel del administrador
- **Dashboard** con indicadores y gráficas (Chart.js).
- **Gestión de citas** en vista de calendario: confirmar, atender, cancelar y registrar la **bitácora del servicio** (tipo de baño, observaciones, productos usados y **fotos de la salida de la mascota**).
- **Productos**: alta, edición y reabastecimiento de inventario. Al subir la foto de un producto se **remueve el fondo automáticamente** para que el catálogo se vea limpio y uniforme.
- **Ventas** de productos desde el panel, con su detalle y descuento de stock.
- **Gastos** por categoría con filtros, paginación y resumen.
- **Finanzas / Reportes**: ingresos, gastos, ganancia y productos más vendidos (en pesos colombianos).
- **Planilla de sanidad**: control diario de limpieza por áreas y herramientas, con reporte de cumplimiento y exportación a PDF.
- Historia clínica, vacunación y desparasitación de las mascotas.

---

## 🧰 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | **Quarkus** (Java 17+) |
| API REST | Quarkus REST + **Jackson** |
| Vistas web | **Qute** (templates del lado del servidor) + HTML, CSS y JavaScript |
| Persistencia | **Hibernate ORM con Panache** (patrón repositorio) |
| Base de datos | **PostgreSQL** (JDBC) |
| Validación | **Hibernate Validator** (Jakarta Validation) |
| Documentación API | **SmallRye OpenAPI + Swagger UI** |
| Seguridad | **BCrypt** para hash de contraseñas, sesión por cookie y filtro por rol |
| Remoción de fondo | **ONNX Runtime (Java)** + modelo **U²-Net** (`models/u2net.onnx`) |
| IA | API de **OpenAI (GPT)** para estimar la duración de las citas |
| PDFs | Generación de PDFs en servidor (encabezado y pie de página personalizados) |
| Gráficas | **Chart.js** |
| Contenedores | **Docker** |
| Build | **Maven** (wrapper `mvnw`) |

---

## 🧱 Arquitectura hexagonal

El código vive en el paquete `usta.veterinaria` y se divide en tres capas. El **dominio no depende de ningún framework**; la infraestructura se conecta a él a través de puertos.

```
usta.veterinaria
├── domain                  → Núcleo del negocio (sin dependencias externas)
│   ├── model               → Entidades y enums: Mascota, AgendamientoCita, Producto, Venta,
│   │                         Gasto, PlanillaSanidad, BitacoraServicio, Usuario...
│   ├── service             → Reglas de negocio puras (CitaService, FinanzasService,
│   │                         PlanillaSanidadService, MonedaColombiana, RelojColombia...)
│   └── exception           → BusinessRuleException
│
├── application             → Casos de uso
│   ├── port/in             → Puertos de entrada (GestionarCitasUseCase, RegistrarVentaUseCase...)
│   ├── port/out            → Puertos de salida (repositorios, PasswordHasher,
│   │                         EstimadorDuracionCitaPort...)
│   └── usecase             → Implementación de los casos de uso
│
└── infrastructure/adapter  → Adaptadores
    ├── in/rest             → API REST: resources, DTOs, mappers y manejo global de errores
    ├── in/web              → Vistas Qute del cliente y del admin, sesión, subida de imágenes
    │                         y remoción de fondo
    └── out
        ├── persistence     → Entidades JPA, repositorios Panache, mappers entidad ↔ dominio
        ├── ia              → Cliente de OpenAI (adaptador de EstimadorDuracionCitaPort)
        └── security        → Implementaciones BCrypt de PasswordHasher / PasswordVerifier
```

Cada módulo (`admin`, `mascota`, `usuario`) se repite en todas las capas para mantener el código organizado por contexto.

**Manejo de errores centralizado:** `ExceptionMapper`s para reglas de negocio, validación, JSON mal formado, errores de persistencia y recursos no encontrados, todos devolviendo un `ErrorResponse` uniforme.

---

## 🖼️ Remoción de fondo de imágenes con U²-Net

Para que las fotos de productos (y mascotas) se vean limpias en el catálogo, al subir una imagen se le **elimina el fondo automáticamente**.

- Se usa el modelo **U²-Net** (*U-Square Net*), red de segmentación de objetos salientes publicada en GitHub ([xuebinqin/U-2-Net](https://github.com/xuebinqin/U-2-Net)), en formato **ONNX** (`models/u2net.onnx`).
- La inferencia se ejecuta **localmente** en Java con **ONNX Runtime**, sin depender de servicios externos.
- Flujo (`QuitarFondo.java` + `ImagenStorage.java`):
  1. Se recibe la imagen subida.
  2. Se redimensiona y normaliza a la entrada del modelo (320×320).
  3. U²-Net genera una máscara de primer plano.
  4. La máscara se escala al tamaño original y se aplica como canal alfa → **PNG con fondo transparente**.
  5. El archivo se guarda en `uploads/productos/` o `uploads/mascotas/` con un nombre UUID y se sirve mediante `UploadsResource`.

> ⚠️ El archivo `u2net.onnx` (~170 MB) debe estar en la carpeta `models/` en la raíz del proyecto. Si no está en el repositorio, descárgalo del repositorio de U²-Net y colócalo ahí.

---

## 🔐 Seguridad y validación por rol

- Contraseñas almacenadas con **BCrypt** (`BCryptPasswordHasher` / `BCryptPasswordVerifier`).
- Sesión mediante **cookie** (`SesionCookie`, `SesionActual`).
- Dos roles (`RolUsuario`): **ADMIN** y **CLIENTE**.
- Las rutas del panel de administración están protegidas con la anotación `@SoloAdmin` y el filtro `SoloAdminFilter`; un cliente no puede acceder a ellas.
- Cada cliente solo ve y gestiona **sus propias mascotas y citas**.
- Validación de entrada con Jakarta Validation en los DTOs y reglas de negocio en el dominio.

---

## 📄 Generación de PDFs

| PDF | Quién lo usa | Contenido |
|---|---|---|
| **Bitácora de la cita** (`BitacoraCitaPdf`) | Cliente | Resumen del último servicio: mascota, tipo de baño, observaciones y fotos de salida |
| **Historial de la mascota** (`HistorialMascotaPdf`) | Admin | Historia clínica, vacunas y desparasitaciones |
| **Planilla de sanidad** (`PlanillaSanidadPdf`) | Admin | Registro de limpieza por área/herramienta y cumplimiento del periodo |

Todos incluyen encabezado de marca y pie de página con numeración.

---

## 🤖 Estimación de duración de citas con IA

Al agendar, el sistema estima cuánto durará el servicio (**ALTA / MEDIA / BAJA**) según el tamaño de la raza y el tipo de pelaje, usando la API de OpenAI a través del puerto `EstimadorDuracionCitaPort` (adaptador `EstimadorDuracionCitaGptAdapter`). Con esa duración se calculan los espacios disponibles en la agenda.

Si el servicio externo falla, `ServicioExternoExceptionMapper` devuelve un error controlado.

---

## 📋 Requisitos

- **Java 17+**
- **Maven 3.9+** (o el wrapper incluido `./mvnw`)
- **PostgreSQL 14+**
- Modelo `models/u2net.onnx`
- API key de OpenAI
- (Opcional) **Docker**

---

## ⚙️ Configuración

Crea un archivo `.env` en la raíz del proyecto (no se sube al repositorio):

```env
DB_URL=jdbc:postgresql://localhost:5432/yazkas
DB_USER=postgres
DB_PASSWORD=tu_password
OPENAI_API_KEY=sk-...
```

> Ajusta los nombres de las variables a los que usa tu `application.properties`.

Datos iniciales se cargan desde `src/main/resources/import.sql`.

---

## ▶️ Ejecución

### Modo desarrollo (live coding)

```shell
./mvnw quarkus:dev
```

- Aplicación: <http://localhost:8080>
- Dev UI: <http://localhost:8080/q/dev/>
- Swagger UI: <http://localhost:8080/q/swagger-ui>

### Empaquetar

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Über-jar:

```shell
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

### Ejecutable nativo

```shell
./mvnw package -Dnative
# o sin GraalVM instalado:
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/yazkas-1.0-SNAPSHOT-runner
```

> Nota: ONNX Runtime usa librerías nativas; la compilación nativa con GraalVM puede requerir configuración adicional.

---

## 🗂️ Estructura del proyecto

```
Folder PATH listing for volume OS
Volume serial number is FC1D-305A
C:.
|   .dockerignore
|   .env
|   .gitignore
|   estructura.txt
|   mvnw
|   mvnw.cmd
|   pom.xml
|   README.md
|   
+---.idea
|   |   .gitignore
|   |   compiler.xml
|   |   dataSources.local.xml
|   |   dataSources.xml
|   |   data_source_mapping.xml
|   |   diff-generator.xml
|   |   encodings.xml
|   |   jarRepositories.xml
|   |   misc.xml
|   |   vcs.xml
|   |   workspace.xml
|   |   
|   \---dataSources
|       |   f749f56d-143f-4b31-8527-aac585d8a01d.xml
|       |   
|       \---f749f56d-143f-4b31-8527-aac585d8a01d
|           \---storage_v2
|               \---_src_
|                   \---database
|                       |   yazkas.K14K1A.meta
|                       |   
|                       \---yazkas.K14K1A
|                           \---schema
|                                   information_schema.FNRwLQ.meta
|                                   pg_catalog.0S1ZNQ.meta
|                                   public.abK9xQ.meta
|                                   
+---.mvn
|   \---wrapper
|           maven-wrapper.properties
|           
+---models
|       u2net.onnx
|       
+---src
|   +---main
|   |   +---java
|   |   |   \---usta
|   |   |       \---veterinaria
|   |   |           +---application
|   |   |           |   +---exception
|   |   |           |   |       ResourceNotFoundException.java
|   |   |           |   |       ServicioExternoExceptionMapper.java
|   |   |           |   |       
|   |   |           |   +---port
|   |   |           |   |   +---in
|   |   |           |   |   |   +---admin
|   |   |           |   |   |   |       ConsultarFinanzasUseCase.java
|   |   |           |   |   |   |       GestionarBitacoraServicioUseCase.java
|   |   |           |   |   |   |       GestionarGastosUseCase.java
|   |   |           |   |   |   |       GestionarPlanillaSanidadUseCase.java
|   |   |           |   |   |   |       GestionarProductosUseCase.java
|   |   |           |   |   |   |       RegistrarVentaUseCase.java
|   |   |           |   |   |   |       
|   |   |           |   |   |   +---mascota
|   |   |           |   |   |   |       GestionarHistoriaClinicaUseCase.java
|   |   |           |   |   |   |       GestionarMascotasUseCase.java
|   |   |           |   |   |   |       GestionarSaludMascotaUseCase.java
|   |   |           |   |   |   |       
|   |   |           |   |   |   \---usuario
|   |   |           |   |   |           AutenticarUsuarioUseCase.java
|   |   |           |   |   |           GestionarCitasUseCase.java
|   |   |           |   |   |           GestionarUsuariosUseCase.java
|   |   |           |   |   |           
|   |   |           |   |   \---out
|   |   |           |   |       +---admin
|   |   |           |   |       |       BitacoraServicioRepository.java
|   |   |           |   |       |       DetalleVentaRepository.java
|   |   |           |   |       |       GastoRepository.java
|   |   |           |   |       |       PlanillaSanidadRepository.java
|   |   |           |   |       |       ProductoRepository.java
|   |   |           |   |       |       VentaRepository.java
|   |   |           |   |       |       
|   |   |           |   |       +---mascota
|   |   |           |   |       |       DesparasitacionRepository.java
|   |   |           |   |       |       HistoriaClinicaRepository.java
|   |   |           |   |       |       MascotaCitaRepository.java
|   |   |           |   |       |       MascotaRepository.java
|   |   |           |   |       |       VacunacionRepository.java
|   |   |           |   |       |       
|   |   |           |   |       \---usuario
|   |   |           |   |               AgendamientoCitaRepository.java
|   |   |           |   |               EstimadorDuracionCitaPort.java
|   |   |           |   |               PasswordHasher.java
|   |   |           |   |               PasswordVerifier.java
|   |   |           |   |               UsuarioRepository.java
|   |   |           |   |               
|   |   |           |   \---usecase
|   |   |           |       |   MascotaFactory.java
|   |   |           |       |   Validaciones.java
|   |   |           |       |   
|   |   |           |       +---admin
|   |   |           |       |       ConsultarFinanzasService.java
|   |   |           |       |       GestionarBitacoraServicioService.java
|   |   |           |       |       GestionarGastosService.java
|   |   |           |       |       GestionarPlanillaSanidadService.java
|   |   |           |       |       GestionarProductosService.java
|   |   |           |       |       RegistrarVentaService.java
|   |   |           |       |       
|   |   |           |       +---mascota
|   |   |           |       |       GestionarHistoriaClinicaService.java
|   |   |           |       |       GestionarMascotasService.java
|   |   |           |       |       GestionarSaludMascotaService.java
|   |   |           |       |       
|   |   |           |       \---usuario
|   |   |           |               AutenticarUsuarioService.java
|   |   |           |               GestionarCitasService.java
|   |   |           |               GestionarUsuariosService.java
|   |   |           |               
|   |   |           +---domain
|   |   |           |   +---exception
|   |   |           |   |       BusinessRuleException.java
|   |   |           |   |       
|   |   |           |   +---model
|   |   |           |   |   +---admin
|   |   |           |   |   |       AreaLimpieza.java
|   |   |           |   |   |       BitacoraServicio.java
|   |   |           |   |   |       CategoriaGasto.java
|   |   |           |   |   |       CategoriaProducto.java
|   |   |           |   |   |       DetalleVenta.java
|   |   |           |   |   |       DiaSanidad.java
|   |   |           |   |   |       Gasto.java
|   |   |           |   |   |       Herramienta.java
|   |   |           |   |   |       PlanillaSanidad.java
|   |   |           |   |   |       Producto.java
|   |   |           |   |   |       TipoBano.java
|   |   |           |   |   |       TipoProducto.java
|   |   |           |   |   |       Venta.java
|   |   |           |   |   |       
|   |   |           |   |   +---mascota
|   |   |           |   |   |       Desparasitacion.java
|   |   |           |   |   |       Energia.java
|   |   |           |   |   |       HistoriaClinica.java
|   |   |           |   |   |       Mascota.java
|   |   |           |   |   |       NivelAgresividad.java
|   |   |           |   |   |       Pelaje.java
|   |   |           |   |   |       Sexo.java
|   |   |           |   |   |       TipoDesparasitacion.java
|   |   |           |   |   |       TipoMascota.java
|   |   |           |   |   |       Vacunacion.java
|   |   |           |   |   |       
|   |   |           |   |   \---usuario
|   |   |           |   |           AgendamientoCita.java
|   |   |           |   |           DuracionCita.java
|   |   |           |   |           EstadoCita.java
|   |   |           |   |           MascotaCita.java
|   |   |           |   |           RolUsuario.java
|   |   |           |   |           Usuario.java
|   |   |           |   |           
|   |   |           |   \---service
|   |   |           |       |   MonedaColombiana.java
|   |   |           |       |   RelojColombia.java
|   |   |           |       |   
|   |   |           |       +---admin
|   |   |           |       |       BitacoraServicioService.java
|   |   |           |       |       FinanzasService.java
|   |   |           |       |       GastoService.java
|   |   |           |       |       PlanillaSanidadService.java
|   |   |           |       |       ProductoService.java
|   |   |           |       |       VentaService.java
|   |   |           |       |       
|   |   |           |       +---macota
|   |   |           |       |       MascotaService.java
|   |   |           |       |       SaludMascotaService.java
|   |   |           |       |       
|   |   |           |       \---usuario
|   |   |           |               CitaService.java
|   |   |           |               UsuarioService.java
|   |   |           |               
|   |   |           \---infrastructure
|   |   |               \---adapter
|   |   |                   +---in
|   |   |                   |   +---rest
|   |   |                   |   |   +---dto
|   |   |                   |   |   |   +---admin
|   |   |                   |   |   |   |       BitacoraServicioRequest.java
|   |   |                   |   |   |   |       BitacoraServicioResponse.java
|   |   |                   |   |   |   |       CumplimientoSanidadResponse.java
|   |   |                   |   |   |   |       DetalleVentaRequest.java
|   |   |                   |   |   |   |       DetalleVentaResponse.java
|   |   |                   |   |   |   |       EspacioDisponibleResponse.java
|   |   |                   |   |   |   |       GastoRequest.java
|   |   |                   |   |   |   |       GastoResponse.java
|   |   |                   |   |   |   |       PlanillaSanidadRequest.java
|   |   |                   |   |   |   |       PlanillaSanidadResponse.java
|   |   |                   |   |   |   |       ProductoCatalogoResponse.java
|   |   |                   |   |   |   |       ProductoRequest.java
|   |   |                   |   |   |   |       ProductoResponse.java
|   |   |                   |   |   |   |       ReabastecerProductoRequest.java
|   |   |                   |   |   |   |       RegistroAdministradorRequest.java
|   |   |                   |   |   |   |       
|   |   |                   |   |   |   +---mascota
|   |   |                   |   |   |   |       DesparasitacionRequest.java
|   |   |                   |   |   |   |       DesparasitacionResponse.java
|   |   |                   |   |   |   |       EdadMascotaResponse.java
|   |   |                   |   |   |   |       HistoriaClinicaRequest.java
|   |   |                   |   |   |   |       HistoriaClinicaResponse.java
|   |   |                   |   |   |   |       VacunacionRequest.java
|   |   |                   |   |   |   |       VacunacionResponse.java
|   |   |                   |   |   |   |       
|   |   |                   |   |   |   \---usuario
|   |   |                   |   |   |           ActualizarUsuarioRequest.java
|   |   |                   |   |   |           AgendamientoCitaResponse.java
|   |   |                   |   |   |           AgendarCitaRequest.java
|   |   |                   |   |   |           RegistroClienteRequest.java
|   |   |                   |   |   |           ReprogramarCitaRequest.java
|   |   |                   |   |   |           UsuarioResponse.java
|   |   |                   |   |   |           
|   |   |                   |   |   +---exception
|   |   |                   |   |   |       BusinessRuleExceptionMapper.java
|   |   |                   |   |   |       ErroresBaseDatos.java
|   |   |                   |   |   |       ErrorResponse.java
|   |   |                   |   |   |       ErrorResponseFactory.java
|   |   |                   |   |   |       GenericExceptionMapper.java
|   |   |                   |   |   |       InvalidFormatExceptionMapper.java
|   |   |                   |   |   |       JsonErrorDetails.java
|   |   |                   |   |   |       JsonMappingExceptionMapper.java
|   |   |                   |   |   |       JsonParseExceptionMapper.java
|   |   |                   |   |   |       MismatchedInputExceptionMapper.java
|   |   |                   |   |   |       PersistenceExceptionMapper.java
|   |   |                   |   |   |       ResourceNotFoundExceptionMapper.java
|   |   |                   |   |   |       ValidationExceptionMapper.java
|   |   |                   |   |   |       WebApplicationExceptionMapper.java
|   |   |                   |   |   |       
|   |   |                   |   |   +---mapper
|   |   |                   |   |   |   |   MapperUtils.java
|   |   |                   |   |   |   |   
|   |   |                   |   |   |   +---admin
|   |   |                   |   |   |   |       FinanzasRestMapper.java
|   |   |                   |   |   |   |       GastoRestMapper.java
|   |   |                   |   |   |   |       PlanillaSanidadRestMapper.java
|   |   |                   |   |   |   |       ProductoRestMapper.java
|   |   |                   |   |   |   |       VentaRestMapper.java
|   |   |                   |   |   |   |       
|   |   |                   |   |   |   +---mascota
|   |   |                   |   |   |   |       HistoriaClinicaRestMapper.java
|   |   |                   |   |   |   |       MascotaRestMapper.java
|   |   |                   |   |   |   |       VacunacionRestMapper.java
|   |   |                   |   |   |   |       
|   |   |                   |   |   |   \---usuario
|   |   |                   |   |   |           AgendamientoCitaRestMapper.java
|   |   |                   |   |   |           MascotaCitaRestMapper.java
|   |   |                   |   |   |           UsuarioRestMapper.java
|   |   |                   |   |   |           
|   |   |                   |   |   \---resource
|   |   |                   |   |       +---admin
|   |   |                   |   |       |       BitacoraServicioResource.java
|   |   |                   |   |       |       FinanzasResource.java
|   |   |                   |   |       |       GastoResource.java
|   |   |                   |   |       |       PlanillaSanidadPdf.java
|   |   |                   |   |       |       PlanillaSanidadResource.java
|   |   |                   |   |       |       ProductoResource.java
|   |   |                   |   |       |       VentaResource.java
|   |   |                   |   |       |       
|   |   |                   |   |       +---mascota
|   |   |                   |   |       |       HistoriaClinicaResource.java
|   |   |                   |   |       |       HistorialMascotaPdf.java
|   |   |                   |   |       |       MascotaResource.java
|   |   |                   |   |       |       SaludMascotaResource.java
|   |   |                   |   |       |       
|   |   |                   |   |       \---usuario
|   |   |                   |   |               CatalogoResource.java
|   |   |                   |   |               CitaResource.java
|   |   |                   |   |               UsuarioResource.java
|   |   |                   |   |               
|   |   |                   |   \---web
|   |   |                   |       |   ImagenStorage.java
|   |   |                   |       |   LoginResource.java
|   |   |                   |       |   LogoutResource.java
|   |   |                   |       |   QuitarFondo.java
|   |   |                   |       |   RegistroResource.java
|   |   |                   |       |   UploadsResource.java
|   |   |                   |       |   
|   |   |                   |       +---admin
|   |   |                   |       |       AdminCitasResource.java
|   |   |                   |       |       AdminGastoResource.java
|   |   |                   |       |       AdminPaginaResource.java
|   |   |                   |       |       AdminPlanillaSanidadResource.java
|   |   |                   |       |       AdminProductoResource.java
|   |   |                   |       |       AdminReportes.java
|   |   |                   |       |       AdminVentasResource.java
|   |   |                   |       |       CalendarioVista.java
|   |   |                   |       |       GastoVista.java
|   |   |                   |       |       
|   |   |                   |       +---sesion
|   |   |                   |       |       SesionActual.java
|   |   |                   |       |       SesionCookie.java
|   |   |                   |       |       SoloAdmin.java
|   |   |                   |       |       SoloAdminFilter.java
|   |   |                   |       |       
|   |   |                   |       \---usuario
|   |   |                   |               AgregarMascotaResource.java
|   |   |                   |               BitacoraCitaPdf.java
|   |   |                   |               CatalogoResource.java
|   |   |                   |               CitasClienteResource.java
|   |   |                   |               ContactoResource.java
|   |   |                   |               HistorialClienteResource.java
|   |   |                   |               NosotrosResource.java
|   |   |                   |               PaginaResource.java
|   |   |                   |               PerfilMascotaResource.java
|   |   |                   |               ServiciosResource.java
|   |   |                   |               
|   |   |                   \---out
|   |   |                       +---persistence
|   |   |                       |   +---adapter
|   |   |                       |   |   +---admin
|   |   |                       |   |   |       JpaBitacoraServicioRepository.java
|   |   |                       |   |   |       JpaDetalleVentaRepository.java
|   |   |                       |   |   |       JpaGastoRepository.java
|   |   |                       |   |   |       JpaPlanillaSanidadRepository.java
|   |   |                       |   |   |       JpaProductoRepository.java
|   |   |                       |   |   |       JpaVentaRepository.java
|   |   |                       |   |   |       
|   |   |                       |   |   +---mascota
|   |   |                       |   |   |       JpaDesparasitacionRepository.java
|   |   |                       |   |   |       JpaHistorialClinicaRepository.java
|   |   |                       |   |   |       JpaMascotaCitaRepository.java
|   |   |                       |   |   |       JpaMascotaRepository.java
|   |   |                       |   |   |       JpaVacunacionRepository.java
|   |   |                       |   |   |       
|   |   |                       |   |   \---usuario
|   |   |                       |   |           JpaAgendamientoRepository.java
|   |   |                       |   |           JpaUsuarioRepository.java
|   |   |                       |   |           
|   |   |                       |   +---entity
|   |   |                       |   |   +---admin
|   |   |                       |   |   |       BitacoraServicioEntity.java
|   |   |                       |   |   |       DetalleVentaEntity.java
|   |   |                       |   |   |       GastoEntity.java
|   |   |                       |   |   |       PlanillaSanidadEntity.java
|   |   |                       |   |   |       ProductoEntity.java
|   |   |                       |   |   |       VentaEntity.java
|   |   |                       |   |   |       
|   |   |                       |   |   +---mascota
|   |   |                       |   |   |       DesparasitacionEntity.java
|   |   |                       |   |   |       HistorialClinicaEntity.java
|   |   |                       |   |   |       MascotaEntity.java
|   |   |                       |   |   |       VacunacionEntity.java
|   |   |                       |   |   |       
|   |   |                       |   |   \---usuario
|   |   |                       |   |           AgendamientoCitaEntity.java
|   |   |                       |   |           MascotaCitaEntity.java
|   |   |                       |   |           UsuarioEntity.java
|   |   |                       |   |           
|   |   |                       |   +---ia
|   |   |                       |   |   |   EstimadorDuracionCitaGptAdapter.java
|   |   |                       |   |   |   
|   |   |                       |   |   \---client
|   |   |                       |   |       |   OpenAiClient.java
|   |   |                       |   |       |   
|   |   |                       |   |       \---dto
|   |   |                       |   |               OpenAiRequest.java
|   |   |                       |   |               OpenAiResponse.java
|   |   |                       |   |               
|   |   |                       |   +---mapper
|   |   |                       |   |   +---admin
|   |   |                       |   |   |       BitacoraServicioEntityMapper.java
|   |   |                       |   |   |       DetalleVentaEntityMapper.java
|   |   |                       |   |   |       GastoEntityMapper.java
|   |   |                       |   |   |       PlanillaSanidadEntityMapper.java
|   |   |                       |   |   |       ProductoEntityMapper.java
|   |   |                       |   |   |       VentaEntityMapper.java
|   |   |                       |   |   |       
|   |   |                       |   |   +---mascota
|   |   |                       |   |   |       DesparasitacionEntityMapper.java
|   |   |                       |   |   |       HistorialClinicaEntityMapper.java
|   |   |                       |   |   |       MascotaEntityMapper.java
|   |   |                       |   |   |       VacunacionEntityMapper.java
|   |   |                       |   |   |       
|   |   |                       |   |   \---usuario
|   |   |                       |   |           AgendamientoCitaEntityMapper.java
|   |   |                       |   |           MascotaCitaEntityMapper.java
|   |   |                       |   |           UsuarioEntityMapper.java
|   |   |                       |   |           
|   |   |                       |   \---repository
|   |   |                       |       +---admin
|   |   |                       |       |       BitacoraServicioJpaRepository.java
|   |   |                       |       |       DetalleVentaJpaRepository.java
|   |   |                       |       |       GastoJpaRepository.java
|   |   |                       |       |       PlanillaSanidadJpaRepository.java
|   |   |                       |       |       ProductoJpaRepository.java
|   |   |                       |       |       VentaJpaRepository.java
|   |   |                       |       |       
|   |   |                       |       +---mascota
|   |   |                       |       |       DesparasitacionJpaRepository.java
|   |   |                       |       |       HistorialClinicaJpaRepository.java
|   |   |                       |       |       MascotaCitaJpaRepository.java
|   |   |                       |       |       MascotaJpaRepository.java
|   |   |                       |       |       VacunacionJpaRepository.java
|   |   |                       |       |       
|   |   |                       |       \---usuario
|   |   |                       |               AgendamientoJpaRepository.java
|   |   |                       |               UsuarioJpaRepository.java
|   |   |                       |               
|   |   |                       \---security
|   |   |                               BCryptPasswordHasher.java
|   |   |                               BCryptPasswordVerifier.java
|   |   |                               
|   |   \---resources
|   |       |   application.properties
|   |       |   import.sql
|   |       |   
|   |       +---META-INF
|   |       |   \---resources
|   |       |       +---css
|   |       |       |   |   login.css
|   |       |       |   |   navbar-sesion.css
|   |       |       |   |   registro.css
|   |       |       |   |   
|   |       |       |   +---admin
|   |       |       |   |       admin-citas.css
|   |       |       |   |       admin-dashboard.css
|   |       |       |   |       admin-gastos.css
|   |       |       |   |       admin-planilla-sanidad.css
|   |       |       |   |       admin-productos.css
|   |       |       |   |       admin-reportes.css
|   |       |       |   |       admin-sidebar.css
|   |       |       |   |       admin-ventas.css
|   |       |       |   |       
|   |       |       |   \---usuarios
|   |       |       |           agendar-cita.css
|   |       |       |           agregar-mascota-pasos.css
|   |       |       |           agregar-mascota.css
|   |       |       |           catalogo.css
|   |       |       |           contacto.css
|   |       |       |           historial.css
|   |       |       |           home.css
|   |       |       |           huellitas.css
|   |       |       |           nav-cliente.css
|   |       |       |           Navbar.css
|   |       |       |           nosotros.css
|   |       |       |           perfil-mascota.css
|   |       |       |           servicios.css
|   |       |       |           
|   |       |       +---img
|   |       |       |   |   fondo-catalogo.png
|   |       |       |   |   fondo-contactanos.png
|   |       |       |   |   fondo-gato-catalogo.png
|   |       |       |   |   fondo-nosotros.png
|   |       |       |   |   logo-pequeno.png
|   |       |       |   |   mascota-services.png
|   |       |       |   |   nariz-mascota.png
|   |       |       |   |   yazkas_login_left_panel.png
|   |       |       |   |   
|   |       |       |   +---admin
|   |       |       |   |       mascota-principal-dash.png
|   |       |       |   |       
|   |       |       |   \---usuario
|   |       |       |           beagle.png
|   |       |       |           Bich≤nMaltΘs.png
|   |       |       |           buscaPremio.jpeg
|   |       |       |           cepillo.jpeg
|   |       |       |           Golden.png
|   |       |       |           JugueteHuespo.jpeg
|   |       |       |           logo.png
|   |       |       |           PerritosAlrededor.png
|   |       |       |           PerritosJuntos.png
|   |       |       |           TrioColin.png
|   |       |       |           TrioHusky.png
|   |       |       |           YorkshireTerrier.png
|   |       |       |           
|   |       |       \---js
|   |       |           |   login.js
|   |       |           |   registro.js
|   |       |           |   
|   |       |           +---admin
|   |       |           |       admin-dashboard.js
|   |       |           |       admin-planilla-sanidad.js
|   |       |           |       admin-productos.js
|   |       |           |       admin-reportes.js
|   |       |           |       admin-sidebar.js
|   |       |           |       admin-ventas.js
|   |       |           |       
|   |       |           +---usuario
|   |       |           |       agendar-cita.js
|   |       |           |       agregar-mascota.js
|   |       |           |       catalogo-hero.js
|   |       |           |       catalogo.js
|   |       |           |       contacto-hero.js
|   |       |           |       contacto.js
|   |       |           |       historial.js
|   |       |           |       home.js
|   |       |           |       huellitas.js
|   |       |           |       Navbar.js
|   |       |           |       nosotros-hero.js
|   |       |           |       nosotros.js
|   |       |           |       perfil-mascota.js
|   |       |           |       
|   |       |           \---vendor
|   |       |                   chart.umd.js
|   |       |                   
|   |       \---templates
|   |           |   login.html
|   |           |   registro.html
|   |           |   
|   |           +---admin
|   |           |   |   adminCitas.html
|   |           |   |   adminGastos.html
|   |           |   |   adminPlanillaSanidad.html
|   |           |   |   adminProductos.html
|   |           |   |   adminReportes.html
|   |           |   |   adminVentas.html
|   |           |   |   indexAdmin.html
|   |           |   |   
|   |           |   \---components
|   |           |           sidebar.html
|   |           |           
|   |           \---usuarios
|   |               |   agendarCita.html
|   |               |   agregarMascota.html
|   |               |   catalogo.html
|   |               |   contacto.html
|   |               |   historialCitas.html
|   |               |   index.html
|   |               |   nosotros.html
|   |               |   perfilMascota.html
|   |               |   servicios.html
|   |               |   
|   |               \---components
|   |                       historial-fila.html
|   |                       nav-cliente.html
|   |                       nav.html
|   |                       
|   \---test
|       \---java
|           \---usta
|                   ExampleResourceIT.java
|                   ExampleResourceTest.java
|                   
+---target
|   |   build-metrics.json
|   |   yazkas-dev.jar
|   |   
|   +---classes
|   |   |   application.properties
|   |   |   import.sql
|   |   |   
|   |   +---META-INF
|   |   |   \---resources
|   |   |       +---css
|   |   |       |   |   login.css
|   |   |       |   |   navbar-sesion.css
|   |   |       |   |   registro.css
|   |   |       |   |   
|   |   |       |   +---admin
|   |   |       |   |       admin-citas.css
|   |   |       |   |       admin-dashboard.css
|   |   |       |   |       admin-gastos.css
|   |   |       |   |       admin-planilla-sanidad.css
|   |   |       |   |       admin-productos.css
|   |   |       |   |       admin-reportes.css
|   |   |       |   |       admin-sidebar.css
|   |   |       |   |       admin-ventas.css
|   |   |       |   |       
|   |   |       |   \---usuarios
|   |   |       |           agendar-cita.css
|   |   |       |           agregar-mascota-pasos.css
|   |   |       |           agregar-mascota.css
|   |   |       |           catalogo.css
|   |   |       |           contacto.css
|   |   |       |           historial.css
|   |   |       |           home.css
|   |   |       |           huellitas.css
|   |   |       |           nav-cliente.css
|   |   |       |           Navbar.css
|   |   |       |           nosotros.css
|   |   |       |           perfil-mascota.css
|   |   |       |           servicios.css
|   |   |       |           
|   |   |       +---img
|   |   |       |   |   fondo-catalogo.png
|   |   |       |   |   fondo-contactanos.png
|   |   |       |   |   fondo-gato-catalogo.png
|   |   |       |   |   fondo-nosotros.png
|   |   |       |   |   logo-pequeno.png
|   |   |       |   |   mascota-services.png
|   |   |       |   |   nariz-mascota.png
|   |   |       |   |   yazkas_login_left_panel.png
|   |   |       |   |   
|   |   |       |   +---admin
|   |   |       |   |       mascota-principal-dash.png
|   |   |       |   |       
|   |   |       |   \---usuario
|   |   |       |           beagle.png
|   |   |       |           Bich≤nMaltΘs.png
|   |   |       |           buscaPremio.jpeg
|   |   |       |           cepillo.jpeg
|   |   |       |           Golden.png
|   |   |       |           JugueteHuespo.jpeg
|   |   |       |           logo.png
|   |   |       |           PerritosAlrededor.png
|   |   |       |           PerritosJuntos.png
|   |   |       |           TrioColin.png
|   |   |       |           TrioHusky.png
|   |   |       |           YorkshireTerrier.png
|   |   |       |           
|   |   |       \---js
|   |   |           |   login.js
|   |   |           |   registro.js
|   |   |           |   
|   |   |           +---admin
|   |   |           |       admin-dashboard.js
|   |   |           |       admin-planilla-sanidad.js
|   |   |           |       admin-productos.js
|   |   |           |       admin-reportes.js
|   |   |           |       admin-sidebar.js
|   |   |           |       admin-ventas.js
|   |   |           |       
|   |   |           +---usuario
|   |   |           |       agendar-cita.js
|   |   |           |       agregar-mascota.js
|   |   |           |       catalogo-hero.js
|   |   |           |       catalogo.js
|   |   |           |       contacto-hero.js
|   |   |           |       contacto.js
|   |   |           |       historial.js
|   |   |           |       home.js
|   |   |           |       huellitas.js
|   |   |           |       Navbar.js
|   |   |           |       nosotros-hero.js
|   |   |           |       nosotros.js
|   |   |           |       perfil-mascota.js
|   |   |           |       
|   |   |           \---vendor
|   |   |                   chart.umd.js
|   |   |                   
|   |   +---templates
|   |   |   |   login.html
|   |   |   |   registro.html
|   |   |   |   
|   |   |   +---admin
|   |   |   |   |   adminCitas.html
|   |   |   |   |   adminGastos.html
|   |   |   |   |   adminPlanillaSanidad.html
|   |   |   |   |   adminProductos.html
|   |   |   |   |   adminReportes.html
|   |   |   |   |   adminVentas.html
|   |   |   |   |   indexAdmin.html
|   |   |   |   |   
|   |   |   |   \---components
|   |   |   |           sidebar.html
|   |   |   |           
|   |   |   \---usuarios
|   |   |       |   agendarCita.html
|   |   |       |   agregarMascota.html
|   |   |       |   catalogo.html
|   |   |       |   contacto.html
|   |   |       |   historialCitas.html
|   |   |       |   index.html
|   |   |       |   nosotros.html
|   |   |       |   perfilMascota.html
|   |   |       |   servicios.html
|   |   |       |   
|   |   |       \---components
|   |   |               historial-fila.html
|   |   |               nav-cliente.html
|   |   |               nav.html
|   |   |               
|   |   \---usta
|   |       \---veterinaria
|   |           +---application
|   |           |   +---exception
|   |           |   |       ResourceNotFoundException.class
|   |           |   |       
|   |           |   +---port
|   |           |   |   +---in
|   |           |   |   |   +---admin
|   |           |   |   |   |       ConsultarFinanzasUseCase.class
|   |           |   |   |   |       GestionarBitacoraServicioUseCase.class
|   |           |   |   |   |       GestionarGastosUseCase.class
|   |           |   |   |   |       GestionarPlanillaSanidadUseCase.class
|   |           |   |   |   |       GestionarProductosUseCase.class
|   |           |   |   |   |       RegistrarVentaUseCase.class
|   |           |   |   |   |       
|   |           |   |   |   +---mascota
|   |           |   |   |   |       GestionarHistoriaClinicaUseCase.class
|   |           |   |   |   |       GestionarMascotasUseCase.class
|   |           |   |   |   |       GestionarSaludMascotaUseCase.class
|   |           |   |   |   |       
|   |           |   |   |   \---usuario
|   |           |   |   |           AutenticarUsuarioUseCase.class
|   |           |   |   |           GestionarCitasUseCase.class
|   |           |   |   |           GestionarUsuariosUseCase.class
|   |           |   |   |           
|   |           |   |   \---out
|   |           |   |       +---admin
|   |           |   |       |       BitacoraServicioRepository.class
|   |           |   |       |       DetalleVentaRepository.class
|   |           |   |       |       GastoRepository.class
|   |           |   |       |       PlanillaSanidadRepository.class
|   |           |   |       |       ProductoRepository.class
|   |           |   |       |       VentaRepository.class
|   |           |   |       |       
|   |           |   |       +---mascota
|   |           |   |       |       DesparasitacionRepository.class
|   |           |   |       |       HistoriaClinicaRepository.class
|   |           |   |       |       MascotaCitaRepository.class
|   |           |   |       |       MascotaRepository.class
|   |           |   |       |       VacunacionRepository.class
|   |           |   |       |       
|   |           |   |       \---usuario
|   |           |   |               AgendamientoCitaRepository.class
|   |           |   |               EstimadorDuracionCitaPort.class
|   |           |   |               PasswordHasher.class
|   |           |   |               PasswordVerifier.class
|   |           |   |               UsuarioRepository.class
|   |           |   |               
|   |           |   \---usecase
|   |           |       |   MascotaFactory.class
|   |           |       |   Validaciones.class
|   |           |       |   
|   |           |       +---admin
|   |           |       |       ConsultarFinanzasService.class
|   |           |       |       GestionarBitacoraServicioService.class
|   |           |       |       GestionarGastosService.class
|   |           |       |       GestionarPlanillaSanidadService.class
|   |           |       |       GestionarProductosService.class
|   |           |       |       RegistrarVentaService.class
|   |           |       |       
|   |           |       +---mascota
|   |           |       |       GestionarHistoriaClinicaService.class
|   |           |       |       GestionarMascotasService.class
|   |           |       |       GestionarSaludMascotaService.class
|   |           |       |       
|   |           |       \---usuario
|   |           |               AutenticarUsuarioService.class
|   |           |               GestionarCitasService.class
|   |           |               GestionarUsuariosService.class
|   |           |               
|   |           +---domain
|   |           |   +---exception
|   |           |   |       BusinessRuleException.class
|   |           |   |       
|   |           |   +---model
|   |           |   |   +---admin
|   |           |   |   |       AreaLimpieza.class
|   |           |   |   |       BitacoraServicio$BitacoraServicioBuilder.class
|   |           |   |   |       BitacoraServicio.class
|   |           |   |   |       CategoriaGasto.class
|   |           |   |   |       CategoriaProducto.class
|   |           |   |   |       DetalleVenta$DetalleVentaBuilder.class
|   |           |   |   |       DetalleVenta.class
|   |           |   |   |       DiaSanidad.class
|   |           |   |   |       Gasto$GastoBuilder.class
|   |           |   |   |       Gasto.class
|   |           |   |   |       Herramienta.class
|   |           |   |   |       PlanillaSanidad$PlanillaSanidadBuilder.class
|   |           |   |   |       PlanillaSanidad.class
|   |           |   |   |       Producto$ProductoBuilder.class
|   |           |   |   |       Producto.class
|   |           |   |   |       TipoBano.class
|   |           |   |   |       TipoProducto.class
|   |           |   |   |       Venta$VentaBuilder.class
|   |           |   |   |       Venta.class
|   |           |   |   |       
|   |           |   |   +---mascota
|   |           |   |   |       Desparasitacion$DesparasitacionBuilder.class
|   |           |   |   |       Desparasitacion.class
|   |           |   |   |       Energia.class
|   |           |   |   |       HistoriaClinica$HistoriaClinicaBuilder.class
|   |           |   |   |       HistoriaClinica.class
|   |           |   |   |       Mascota$MascotaBuilder.class
|   |           |   |   |       Mascota.class
|   |           |   |   |       NivelAgresividad.class
|   |           |   |   |       Pelaje.class
|   |           |   |   |       Sexo.class
|   |           |   |   |       TipoDesparasitacion.class
|   |           |   |   |       TipoMascota.class
|   |           |   |   |       Vacunacion$VacunacionBuilder.class
|   |           |   |   |       Vacunacion.class
|   |           |   |   |       
|   |           |   |   \---usuario
|   |           |   |           AgendamientoCita$AgendamientoCitaBuilder.class
|   |           |   |           AgendamientoCita.class
|   |           |   |           DuracionCita.class
|   |           |   |           EstadoCita.class
|   |           |   |           MascotaCita$MascotaCitaBuilder.class
|   |           |   |           MascotaCita.class
|   |           |   |           RolUsuario.class
|   |           |   |           Usuario$UsuarioBuilder.class
|   |           |   |           Usuario.class
|   |           |   |           
|   |           |   \---service
|   |           |       |   MonedaColombiana.class
|   |           |       |   RelojColombia.class
|   |           |       |   
|   |           |       +---admin
|   |           |       |       BitacoraServicioService.class
|   |           |       |       FinanzasService$Acumulado.class
|   |           |       |       FinanzasService$ProductoVendido.class
|   |           |       |       FinanzasService$PuntoSerie.class
|   |           |       |       FinanzasService.class
|   |           |       |       GastoService.class
|   |           |       |       PlanillaSanidadService$1.class
|   |           |       |       PlanillaSanidadService.class
|   |           |       |       ProductoService.class
|   |           |       |       VentaService.class
|   |           |       |       
|   |           |       +---macota
|   |           |       |       MascotaService.class
|   |           |       |       SaludMascotaService.class
|   |           |       |       
|   |           |       \---usuario
|   |           |               CitaService$1.class
|   |           |               CitaService.class
|   |           |               UsuarioService.class
|   |           |               
|   |           \---infrastructure
|   |               +---adapter
|   |               |   +---in
|   |               |   |   +---rest
|   |               |   |   |   +---dto
|   |               |   |   |   |   +---admin
|   |               |   |   |   |   |       BitacoraServicioRequest.class
|   |               |   |   |   |   |       BitacoraServicioResponse$GananciaProductoResponse.class
|   |               |   |   |   |   |       BitacoraServicioResponse.class
|   |               |   |   |   |   |       CumplimientoSanidadResponse$VentaRequest.class
|   |               |   |   |   |   |       CumplimientoSanidadResponse$VentaResponse.class
|   |               |   |   |   |   |       CumplimientoSanidadResponse.class
|   |               |   |   |   |   |       DetalleVentaRequest$ResumenFinancieroResponse.class
|   |               |   |   |   |   |       DetalleVentaRequest.class
|   |               |   |   |   |   |       DetalleVentaResponse.class
|   |               |   |   |   |   |       EspacioDisponibleResponse.class
|   |               |   |   |   |   |       GastoRequest.class
|   |               |   |   |   |   |       GastoResponse.class
|   |               |   |   |   |   |       PlanillaSanidadRequest.class
|   |               |   |   |   |   |       PlanillaSanidadResponse.class
|   |               |   |   |   |   |       ProductoCatalogoResponse.class
|   |               |   |   |   |   |       ProductoRequest.class
|   |               |   |   |   |   |       ProductoResponse.class
|   |               |   |   |   |   |       ReabastecerProductoRequest.class
|   |               |   |   |   |   |       RegistroAdministradorRequest.class
|   |               |   |   |   |   |       
|   |               |   |   |   |   +---mascota
|   |               |   |   |   |   |       DesparasitacionRequest.class
|   |               |   |   |   |   |       DesparasitacionResponse$MascotaCitaResponse.class
|   |               |   |   |   |   |       DesparasitacionResponse$MascotaRequest.class
|   |               |   |   |   |   |       DesparasitacionResponse$MascotaResponse.class
|   |               |   |   |   |   |       DesparasitacionResponse.class
|   |               |   |   |   |   |       EdadMascotaResponse.class
|   |               |   |   |   |   |       HistoriaClinicaRequest.class
|   |               |   |   |   |   |       HistoriaClinicaResponse.class
|   |               |   |   |   |   |       VacunacionRequest.class
|   |               |   |   |   |   |       VacunacionResponse.class
|   |               |   |   |   |   |       
|   |               |   |   |   |   \---usuario
|   |               |   |   |   |           ActualizarUsuarioRequest.class
|   |               |   |   |   |           AgendamientoCitaResponse.class
|   |               |   |   |   |           AgendarCitaRequest.class
|   |               |   |   |   |           RegistroClienteRequest.class
|   |               |   |   |   |           ReprogramarCitaRequest.class
|   |               |   |   |   |           UsuarioResponse.class
|   |               |   |   |   |           
|   |               |   |   |   +---exception
|   |               |   |   |   |       BusinessRuleExceptionMapper.class
|   |               |   |   |   |       ErroresBaseDatos.class
|   |               |   |   |   |       ErrorResponse.class
|   |               |   |   |   |       ErrorResponseFactory.class
|   |               |   |   |   |       GenericExceptionMapper.class
|   |               |   |   |   |       InvalidFormatExceptionMapper.class
|   |               |   |   |   |       JsonErrorDetails.class
|   |               |   |   |   |       JsonMappingExceptionMapper.class
|   |               |   |   |   |       JsonParseExceptionMapper.class
|   |               |   |   |   |       MismatchedInputExceptionMapper.class
|   |               |   |   |   |       PersistenceExceptionMapper.class
|   |               |   |   |   |       ResourceNotFoundExceptionMapper.class
|   |               |   |   |   |       ValidationExceptionMapper.class
|   |               |   |   |   |       WebApplicationExceptionMapper.class
|   |               |   |   |   |       
|   |               |   |   |   +---mapper
|   |               |   |   |   |   |   MapperUtils.class
|   |               |   |   |   |   |   
|   |               |   |   |   |   +---admin
|   |               |   |   |   |   |       FinanzasRestMapper$BitacoraServicioRestMapper.class
|   |               |   |   |   |   |       FinanzasRestMapper.class
|   |               |   |   |   |   |       GastoRestMapper.class
|   |               |   |   |   |   |       PlanillaSanidadRestMapper.class
|   |               |   |   |   |   |       ProductoRestMapper$DesparasitacionRestMapper.class
|   |               |   |   |   |   |       ProductoRestMapper.class
|   |               |   |   |   |   |       VentaRestMapper.class
|   |               |   |   |   |   |       
|   |               |   |   |   |   +---mascota
|   |               |   |   |   |   |       HistoriaClinicaRestMapper.class
|   |               |   |   |   |   |       MascotaRestMapper.class
|   |               |   |   |   |   |       VacunacionRestMapper.class
|   |               |   |   |   |   |       
|   |               |   |   |   |   \---usuario
|   |               |   |   |   |           AgendamientoCitaRestMapper.class
|   |               |   |   |   |           MascotaCitaRestMapper.class
|   |               |   |   |   |           UsuarioRestMapper.class
|   |               |   |   |   |           
|   |               |   |   |   \---resource
|   |               |   |   |       +---admin
|   |               |   |   |       |       BitacoraServicioResource.class
|   |               |   |   |       |       FinanzasResource.class
|   |               |   |   |       |       GastoResource.class
|   |               |   |   |       |       HistorialMascotaPdf$PiePagina.class
|   |               |   |   |       |       HistorialMascotaPdf.class
|   |               |   |   |       |       PlanillaSanidadPdf$PiePagina.class
|   |               |   |   |       |       PlanillaSanidadPdf.class
|   |               |   |   |       |       PlanillaSanidadResource$Opcion.class
|   |               |   |   |       |       PlanillaSanidadResource$Opciones.class
|   |               |   |   |       |       PlanillaSanidadResource.class
|   |               |   |   |       |       ProductoResource.class
|   |               |   |   |       |       VentaResource.class
|   |               |   |   |       |       
|   |               |   |   |       +---mascota
|   |               |   |   |       |       HistoriaClinicaResource.class
|   |               |   |   |       |       MascotaResource.class
|   |               |   |   |       |       SaludMascotaResource.class
|   |               |   |   |       |       
|   |               |   |   |       \---usuario
|   |               |   |   |               CatalogoResource.class
|   |               |   |   |               CitaResource.class
|   |               |   |   |               UsuarioResource.class
|   |               |   |   |               
|   |               |   |   \---web
|   |               |   |       |   ImagenStorage.class
|   |               |   |       |   LoginResource$1.class
|   |               |   |       |   LoginResource.class
|   |               |   |       |   LogoutResource.class
|   |               |   |       |   QuitarFondo.class
|   |               |   |       |   RegistroResource.class
|   |               |   |       |   UploadsResource$UrlImagenResponse.class
|   |               |   |       |   UploadsResource.class
|   |               |   |       |   
|   |               |   |       +---admin
|   |               |   |       |       AdminCitasResource$1.class
|   |               |   |       |       AdminCitasResource$Grupo.class
|   |               |   |       |       AdminCitasResource.class
|   |               |   |       |       AdminGastoResource$1.class
|   |               |   |       |       AdminGastoResource$Filtro.class
|   |               |   |       |       AdminGastoResource.class
|   |               |   |       |       AdminPaginaResource.class
|   |               |   |       |       AdminPlanillaSanidadResource.class
|   |               |   |       |       AdminProductoResource.class
|   |               |   |       |       AdminReportes$FilaReporte.class
|   |               |   |       |       AdminReportes.class
|   |               |   |       |       AdminVentasResource.class
|   |               |   |       |       CalendarioVista$AccionVista.class
|   |               |   |       |       CalendarioVista$BitacoraVista.class
|   |               |   |       |       CalendarioVista$BloqueVista.class
|   |               |   |       |       CalendarioVista$CeldaVista.class
|   |               |   |       |       CalendarioVista$ChipVista.class
|   |               |   |       |       CalendarioVista$DesparasitacionVista.class
|   |               |   |       |       CalendarioVista$DiaVista.class
|   |               |   |       |       CalendarioVista$MascotaVista.class
|   |               |   |       |       CalendarioVista$OpcionVista.class
|   |               |   |       |       CalendarioVista$ResumenVista.class
|   |               |   |       |       CalendarioVista$VacunaVista.class
|   |               |   |       |       CalendarioVista.class
|   |               |   |       |       GastoVista$AtajoVista.class
|   |               |   |       |       GastoVista$CategoriaVista.class
|   |               |   |       |       GastoVista$DesgloseVista.class
|   |               |   |       |       GastoVista$DialogoVista.class
|   |               |   |       |       GastoVista$EliminarVista.class
|   |               |   |       |       GastoVista$FilaVista.class
|   |               |   |       |       GastoVista$FiltroVista.class
|   |               |   |       |       GastoVista$PaginacionVista.class
|   |               |   |       |       GastoVista$PaginaItemVista.class
|   |               |   |       |       GastoVista$ResumenVista.class
|   |               |   |       |       GastoVista.class
|   |               |   |       |       
|   |               |   |       +---sesion
|   |               |   |       |       SesionActual.class
|   |               |   |       |       SesionCookie$DatosSesion.class
|   |               |   |       |       SesionCookie.class
|   |               |   |       |       SoloAdmin.class
|   |               |   |       |       SoloAdminFilter.class
|   |               |   |       |       
|   |               |   |       \---usuario
|   |               |   |               AgregarMascotaResource.class
|   |               |   |               BitacoraCitaPdf$PiePagina.class
|   |               |   |               BitacoraCitaPdf.class
|   |               |   |               CatalogoResource.class
|   |               |   |               CitasClienteResource$1.class
|   |               |   |               CitasClienteResource$CitaVista.class
|   |               |   |               CitasClienteResource$MascotaVista.class
|   |               |   |               CitasClienteResource.class
|   |               |   |               ContactoResource.class
|   |               |   |               HistorialClienteResource$1.class
|   |               |   |               HistorialClienteResource$CitaConBitacora.class
|   |               |   |               HistorialClienteResource$CitaFila.class
|   |               |   |               HistorialClienteResource$MascotaHistorial.class
|   |               |   |               HistorialClienteResource$UltimaVista.class
|   |               |   |               HistorialClienteResource.class
|   |               |   |               PaginaResource.class
|   |               |   |               PerfilMascotaResource.class
|   |               |   |               ServiciosResource.class
|   |               |   |               
|   |               |   \---out
|   |               |       +---persistence
|   |               |       |   +---adapter
|   |               |       |   |   +---admin
|   |               |       |   |   |       JpaBitacoraServicioRepository.class
|   |               |       |   |   |       JpaDetalleVentaRepository.class
|   |               |       |   |   |       JpaGastoRepository.class
|   |               |       |   |   |       JpaPlanillaSanidadRepository.class
|   |               |       |   |   |       JpaProductoRepository.class
|   |               |       |   |   |       JpaVentaRepository.class
|   |               |       |   |   |       
|   |               |       |   |   +---mascota
|   |               |       |   |   |       JpaDesparasitacionRepository.class
|   |               |       |   |   |       JpaHistorialClinicaRepository.class
|   |               |       |   |   |       JpaMascotaCitaRepository.class
|   |               |       |   |   |       JpaMascotaRepository.class
|   |               |       |   |   |       JpaVacunacionRepository.class
|   |               |       |   |   |       
|   |               |       |   |   \---usuario
|   |               |       |   |           JpaAgendamientoRepository.class
|   |               |       |   |           JpaUsuarioRepository.class
|   |               |       |   |           
|   |               |       |   +---entity
|   |               |       |   |   +---admin
|   |               |       |   |   |       BitacoraServicioEntity.class
|   |               |       |   |   |       DetalleVentaEntity.class
|   |               |       |   |   |       GastoEntity.class
|   |               |       |   |   |       PlanillaSanidadEntity.class
|   |               |       |   |   |       ProductoEntity.class
|   |               |       |   |   |       VentaEntity.class
|   |               |       |   |   |       
|   |               |       |   |   +---mascota
|   |               |       |   |   |       DesparasitacionEntity.class
|   |               |       |   |   |       HistorialClinicaEntity.class
|   |               |       |   |   |       MascotaEntity.class
|   |               |       |   |   |       VacunacionEntity.class
|   |               |       |   |   |       
|   |               |       |   |   \---usuario
|   |               |       |   |           AgendamientoCitaEntity.class
|   |               |       |   |           MascotaCitaEntity.class
|   |               |       |   |           UsuarioEntity.class
|   |               |       |   |           
|   |               |       |   +---ia
|   |               |       |   |   |   EstimadorDuracionCitaGptAdapter.class
|   |               |       |   |   |   
|   |               |       |   |   \---client
|   |               |       |   |       |   OpenAiClient.class
|   |               |       |   |       |   
|   |               |       |   |       \---dto
|   |               |       |   |               OpenAiRequest$Message.class
|   |               |       |   |               OpenAiRequest.class
|   |               |       |   |               OpenAiResponse$Choice.class
|   |               |       |   |               OpenAiResponse$Message.class
|   |               |       |   |               OpenAiResponse.class
|   |               |       |   |               
|   |               |       |   +---mapper
|   |               |       |   |   +---admin
|   |               |       |   |   |       BitacoraServicioEntityMapper.class
|   |               |       |   |   |       DetalleVentaEntityMapper.class
|   |               |       |   |   |       GastoEntityMapper.class
|   |               |       |   |   |       PlanillaSanidadEntityMapper.class
|   |               |       |   |   |       ProductoEntityMapper.class
|   |               |       |   |   |       VentaEntityMapper.class
|   |               |       |   |   |       
|   |               |       |   |   +---mascota
|   |               |       |   |   |       DesparasitacionEntityMapper.class
|   |               |       |   |   |       HistorialClinicaEntityMapper.class
|   |               |       |   |   |       MascotaEntityMapper.class
|   |               |       |   |   |       VacunacionEntityMapper.class
|   |               |       |   |   |       
|   |               |       |   |   \---usuario
|   |               |       |   |           AgendamientoCitaEntityMapper.class
|   |               |       |   |           MascotaCitaEntityMapper.class
|   |               |       |   |           UsuarioEntityMapper.class
|   |               |       |   |           
|   |               |       |   \---repository
|   |               |       |       +---admin
|   |               |       |       |       BitacoraServicioJpaRepository.class
|   |               |       |       |       DetalleVentaJpaRepository.class
|   |               |       |       |       GastoJpaRepository.class
|   |               |       |       |       PlanillaSanidadJpaRepository.class
|   |               |       |       |       ProductoJpaRepository.class
|   |               |       |       |       VentaJpaRepository.class
|   |               |       |       |       
|   |               |       |       +---mascota
|   |               |       |       |       DesparasitacionJpaRepository.class
|   |               |       |       |       HistorialClinicaJpaRepository.class
|   |               |       |       |       MascotaCitaJpaRepository.class
|   |               |       |       |       MascotaJpaRepository.class
|   |               |       |       |       VacunacionJpaRepository.class
|   |               |       |       |       
|   |               |       |       \---usuario
|   |               |       |               AgendamientoJpaRepository.class
|   |               |       |               UsuarioJpaRepository.class
|   |               |       |               
|   |               |       \---security
|   |               |               BCryptPasswordHasher.class
|   |               |               BCryptPasswordVerifier.class
|   |               |               
|   |               \---web
|   |                       NosotrosResource.class
|   |                       
|   +---generated-sources
|   |   \---annotations
|   +---generated-test-sources
|   |   \---test-annotations
|   +---maven-status
|   |   \---maven-compiler-plugin
|   |       +---compile
|   |       |   \---null
|   |       |           createdFiles.lst
|   |       |           inputFiles.lst
|   |       |           
|   |       \---testCompile
|   |           \---null
|   |                   createdFiles.lst
|   |                   inputFiles.lst
|   |                   
|   +---quarkus
|   |   \---bootstrap
|   |           dev-app-model.dat
|   |           test-app-model.dat
|   |           
|   \---test-classes
|       \---usta
|               ExampleResourceIT.class
|               ExampleResourceTest.class
|               
\---uploads
    +---mascotas
    |       008cbd795f614c8690e5f1797c9eaef8.png
    |       07f72610921a40aeb985063076904264.png
    |       2eee5edd90904de5a82d28b850d3ea14.png
    |       466a62dc6d324a448701fbed21f037e1.jpg
    |       8bb3d42044d54974b431d91949db3b07.png
    |       ba4afbf4c988418a81516ee01da9c91b.png
    |       cd3abfde64b4445291e672f354a7252e.png
    |       f222dbd92b54439bb9e3bc37043d8f05.jpg
    |       f79575d6be0f4fae9193d82d313a4fd1.png
    |       
    \---productos
            08bf583db329433dafd2035280cf396e.png
            34be9bbfa9754f2db0a904a8061b1415.png
            

```

---

## 📏 Reglas de negocio

- Horario de atención: **lunes a sábado, 8:30 a. m. – 5:30 p. m.**; toda cita debe terminar antes del cierre.
- Zona horaria de Colombia (`RelojColombia`) y valores en **pesos colombianos** (`MonedaColombiana`).
- No se pueden agendar citas en horarios ocupados ni en el pasado.
- El catálogo público no muestra precios; las ventas se registran desde el panel de admin.
- Las ventas descuentan stock; no se puede vender más de lo disponible.

---

## 👥 Autores

Proyecto — **Universidad Santo Tomás (USTA)**.

<!-- Javier Esteban López Fonseca  -->