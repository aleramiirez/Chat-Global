# Proyecto de Aplicación Chat

## Descripción del Proyecto

Este proyecto implementa una aplicación simple de chat con un servidor y un cliente en Java utilizando JavaFX para la interfaz gráfica y DatagramSocket para la comunicación sobre UDP.

## Servidor

### `Server.java`

La aplicación del servidor escucha mensajes entrantes de clientes, gestiona el registro de usuarios y reenvía mensajes a todos los clientes conectados excepto al remitente. También puede manejar mensajes de imágenes, guardándolos en un directorio especificado.

#### Dependencias:

- JavaFX

#### Cómo Ejecutar:

1. Compila y ejecuta la clase `Server`.

2. El servidor comenzará a escuchar en el puerto especificado.

3. La GUI del servidor muestra mensajes entrantes y registros del servidor.

## Cliente

### `ChatClient.java`

La aplicación del cliente permite a los usuarios enviar mensajes de texto e imágenes al servidor. Se comunica con el servidor utilizando DatagramSocket sobre UDP. La GUI está implementada utilizando JavaFX.

#### Dependencias:

- JavaFX

#### Cómo Ejecutar:

1. Compila y ejecuta la clase `ChatClient`.

2. Ingresa un nombre de usuario cuando se te solicite. El cliente validará el nombre de usuario con el servidor.

3. La GUI del cliente muestra mensajes entrantes y permite al usuario enviar mensajes de texto o imágenes.

## Notas Importantes:

- El servidor y el cliente deben ejecutarse en la misma máquina o en la misma red local.

- Las imágenes se guardan en el directorio especificado en el servidor.

- Los registros del servidor muestran información sobre mensajes entrantes, registros de usuarios y el guardado de imágenes.

Siéntete libre de personalizar y ampliar este proyecto según tus necesidades.
