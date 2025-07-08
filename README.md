# Gerador de QR Code em Java com Suporte a Logo

Este é um projeto em Java puro e sem dependências externas para a geração de QR Codes a partir do zero. A biblioteca gera uma representação vetorial (SVG) do QR Code, com a funcionalidade de adicionar um logo personalizado no centro da imagem.

## ✨ Funcionalidades

  - **Sem Dependências**: Funciona de forma autônoma, sem a necessidade de bibliotecas de terceiros.
  - **Saída em SVG**: Gera QR Codes como imagens SVG (Scalable Vector Graphics), que são leves e escaláveis sem perda de qualidade.
  - **Codificação Inteligente**: Detecta e aplica automaticamente o modo de codificação mais eficiente para os dados de entrada (Numérico, Alfanumérico ou Byte).
  - **Máscara de Otimização**: Implementa e testa as 8 máscaras de padrão do QR Code, selecionando a melhor opção para garantir a máxima legibilidade por qualquer leitor.
  - **Suporte a Logo**: Permite embutir facilmente uma imagem (através de um link) no centro do QR Code, criando uma área de exclusão para não comprometer a leitura.
  - **Níveis de Correção de Erro**: Suporte para os níveis de correção de erro (ECL) que permitem que o QR Code seja lido mesmo se estiver danificado.

## 🛠️ Requisitos

  - Java 10 ou superior (devido ao uso da inferência de tipo `var`).

## 🚀 Como Usar

Para usar o gerador, basta instanciar a classe `QrCode` e chamar o método `generateSvg`.

**Exemplo de código:**

```java
import com.esotar.api.utils.QrCode;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Instancia o gerador
        QrCode qrCodeGenerator = new QrCode();

        // Dados para o QR Code
        String data = "https://www.google.com";
        int dimension = 512; // dimensão da imagem em pixels
        String logoUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"; // opcional

        try {
            // Gera o SVG do QR Code
            String qrCodeSvg = qrCodeGenerator.generateSvg(data, dimension, logoUrl);

            // Imprime o SVG no console
            System.out.println("SVG Gerado:");
            System.out.println(qrCodeSvg);

            // Salva o SVG em um arquivo
            try (FileWriter writer = new FileWriter("qrcode.svg")) {
                writer.write(qrCodeSvg);
                System.out.println("\nArquivo 'qrcode.svg' salvo com sucesso!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Assinatura do Método Principal

```java
public String generateSvg(String value, int dimension, String logoLink)
```

  - `value` (String): O conteúdo que você deseja codificar no QR Code (um link, um texto, etc.).
  - `dimension` (int): A largura e altura desejadas para a imagem SVG final, em pixels.
  - `logoLink` (String): A URL da imagem que será embutida no centro. Se não desejar um logo, passe `null` ou uma string vazia.

### Exemplo de Saída (SVG)

O método retorna uma string contendo o código SVG completo, pronta para ser salva em um arquivo `.svg` ou renderizada em um componente web.

```xml
<svg preserveAspectRatio="none" viewBox="0 0 33 33" width="512" height="512" fill="#000" shape-rendering="crispEdges" xmlns="http://www.w3.org/2000/svg" version="1.1">
    <path transform="matrix(1,0,0,1,1,1)" d="M0,0h1v1h-1v-1zM2,0h1v1h-1v-1zM3,0h1v1h-1v-1z... (caminho dos dados) ... z"/>
    <image xlink:href="https://link-para-seu-logo.com/logo.png" height="5" width="5" x="13" y="13" preserveAspectRatio="none"></image>
</svg>
```
