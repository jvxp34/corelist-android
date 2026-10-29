# Modelos da API (kotlinx.serialization). As bibliotecas já trazem as regras
# principais; isto mantém os nomes dos campos das classes serializáveis.
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class br.com.comprafacil.**$$serializer { *; }
-keepclassmembers class br.com.comprafacil.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.comprafacil.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Interfaces do Retrofit
-keep,allowobfuscation interface br.com.comprafacil.data.api.** { *; }
