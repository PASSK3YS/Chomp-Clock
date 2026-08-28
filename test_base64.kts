import java.io.File
import java.util.Base64

val base64File = File("debug.keystore.base64")
val localKeystore = File("debug_local.keystore")
val base64String = base64File.readText().replace("\n", "").replace("\r", "")
val decodedBytes = Base64.getDecoder().decode(base64String)
localKeystore.writeBytes(decodedBytes)
