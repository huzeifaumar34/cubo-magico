# Cubo Mágico

Aplicativo Android 3x3 com a assinatura **CRIADO POR HUZEIFA**.

## Funcionalidades integradas nesta primeira versão

- Splash de abertura e interface escura com Jetpack Compose.
- Pré-visualização 3D OpenGL ES 2.0 com rotação por toque.
- Editor manual de 54 adesivos com centros fixos e validação de contagem.
- Solver two-phase min2phaseCXX ligado por JNI/C++.
- Gerador de embaralhamentos de 20 a 25 movimentos.
- Cronómetro com registo local, penalidades +2 e DNF.
- Histórico de tempos e estatísticas básicas usando Room.
- ABI armeabi-v7a (32-bit) e arm64-v8a (64-bit).
- GitHub Actions para compilar o APK debug e o publicar como artefacto.

## Obter o APK

Cada push para main, pull request ou execução manual inicia o workflow Android Build.
No GitHub, abre Actions, seleciona a execução mais recente e descarrega o artefacto Cubo-Magico-debug-apk.

O APK debug pode ser instalado para testes, mas não é uma release assinada para distribuição pública. A assinatura de produção exige um keystore guardado nos GitHub Secrets.

## Requisitos do build

JDK 17, Gradle 8.9, Android SDK 35, NDK 26.1.10909125 e CMake 3.22.1.

## Próximas melhorias

A captura de cores pela câmara, o método humano por camadas e a animação sincronizada de cada movimento ainda precisam de desenvolvimento e validação em aparelhos reais.
