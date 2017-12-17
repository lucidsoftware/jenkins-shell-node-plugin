-allowaccessmodification
-keep public class com.lucidchart.jenkins.commandnode.** {
  public *;
}
-keep,allowoptimization,allowshrinking class * { *; }
-keepattributes *
-optimizations !method/inlining/* # VerifyError: interface method reference is in an indirect superinterface
-optimizationpasses 3
-verbose
