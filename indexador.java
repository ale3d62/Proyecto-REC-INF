import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.lang.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.stream.Collectors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.tartarus.snowball.ext.englishStemmer;

class indexador{

    //PARAMETROS
    public static String RUTA_DOCUMENTOS = "corpus";
    public static Set<String> palabrasVacias = new HashSet<String>();

    //VARIABLES GLOBALES
    public static String[] documentos;
    public static Map<String, Object[]> indiceInvertido = new HashMap<String, Object[]>();
    public static Map<String, Integer> apariciones = new HashMap<String, Integer>();
    public static Set<String> terminos = new HashSet<String>();



    public static String preprocesarTexto(String texto){

        texto = texto.toLowerCase();
        texto = texto.replaceAll("([^-áéíóúñ\\w])|_", " ");
        texto = texto.replaceAll("\\b[0-9]+\\b", " ");
        texto = texto.replaceAll("-+ | -+", " ");
        texto = texto.replaceAll(" +", " ");

        return texto;
    }



    public static List<String> preprocesarTerminos(List<String> terminos){
        return terminos.stream().filter(termino -> !palabrasVacias.contains(termino)).collect(Collectors.toList());
    }

    

    public static List<String> eliminarTerminosCortos(List<String> terminos){
        return terminos.stream().filter(termino -> termino.length() >= 3).collect(Collectors.toList());
    }



    public static List<String> stemming(List<String> terminos){
        englishStemmer Estemmer = new englishStemmer();

        for(int i = 0; i<terminos.size(); i++){
            if (! terminos.get(i).contains("-")) {
				Estemmer.setCurrent(terminos.get(i));
				Estemmer.stem();
				terminos.set(i, Estemmer.getCurrent());
			}
        }

        return terminos;
    }





    //Lee el contenido del documento y devuelve los terminos relevantes
    public static List<String> obtenerTerminos(String documento){
        Path pathDocumento = Paths.get(RUTA_DOCUMENTOS+ "/" + documento);

        try{
            String texto = Files.readString(pathDocumento, StandardCharsets.UTF_8);
            texto = preprocesarTexto(texto);

            List<String> terminos = Arrays.asList(texto.split(" "));
            terminos = preprocesarTerminos(terminos);
            terminos = stemming(terminos);
            terminos = eliminarTerminosCortos(terminos);

            return terminos;
        }
        catch (IOException e){
            System.out.println("ERROR EN LA LECTURA DE: " + documento);
        }

        return Collections.emptyList();
    } 



    public static void cargarPalabrasVacias(){
        try (BufferedReader br = new BufferedReader(new FileReader("palabras_vacias.txt"))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                palabrasVacias.add(linea.trim());
            }
        } catch (IOException e) {
            System.out.println("ERROR EN LA LECTURA DE PALABRAS VACIAS");
        }
    }





    public static void exportarLongDocumento(Map<String, Object[]> indiceInvertido){
        
        //Calcular longitudes
        Map<String, Float> longDocumentos = new HashMap<String, Float>();

        for(Map.Entry<String, Object[]> entry : indiceInvertido.entrySet()){
            HashMap<String, Float> hashmap = (HashMap<String, Float>)entry.getValue()[1];
            for(Map.Entry<String, Float> subEntry : hashmap.entrySet()){
                String documento = subEntry.getKey();
                if(!longDocumentos.containsKey(documento))
                    longDocumentos.put(documento, (float)Math.pow(subEntry.getValue(), 2));
                else
                    longDocumentos.put(documento, longDocumentos.get(documento) + (float)Math.pow(subEntry.getValue(), 2));
            }
        }

        for(Map.Entry<String, Float> entry : longDocumentos.entrySet()){
            longDocumentos.put(entry.getKey(), (float)Math.sqrt(entry.getValue()));
        }


        //Exportar
        int nDocumentos = longDocumentos.size();
        File file = new File("longDocumentos.json"); 
        
        try{
            BufferedWriter bf = new BufferedWriter(new FileWriter(file));

            bf.write("{");
            int i = 1;

            for (Map.Entry<String, Float> entry : longDocumentos.entrySet()){
                bf.write("\"" + entry.getKey() + "\":" + entry.getValue());
                if(i < nDocumentos){
                    bf.write(",");
                }
                i++;
                bf.newLine();
            }
            bf.write("}");
            bf.flush();
            bf.close(); 
        }
        catch(IOException e){
            //e.printStackTrace();
            System.out.println("ERROR DURANTE LA EXPORACION DE LONGDOCUMENTOS");
        }
    }



    public static void exportarIndiceInvertido(Map<String, Object[]> indiceInvertido){
        File file = new File("indice_invertido.json");

        try{
            BufferedWriter bf = new BufferedWriter(new FileWriter(file));

            bf.write("{");
            int i = 1;
            int nTerminos = indiceInvertido.size();

            for (Map.Entry<String, Object[]> entry : indiceInvertido.entrySet()){
                bf.write("\"" + entry.getKey() + "\": [" + entry.getValue()[0] + ", {");

                HashMap<String, Float> hashmap = (HashMap<String, Float>)entry.getValue()[1];
                int j = 1;
                int nDocumentos = hashmap.size();
                for (Map.Entry<String, Float> subEntry : hashmap.entrySet()){
                    bf.write("\"" + subEntry.getKey() + "\": " + subEntry.getValue());

                    if(j < nDocumentos){
                        bf.write(",");
                    }
                    j++;
                }
                bf.write("}]");

                if(i < nTerminos){
                    bf.write(",");
                }
                i++;
                bf.newLine();
            }
            bf.write("}");
            bf.flush();
            bf.close(); 
        }
        catch(IOException e){
            //e.printStackTrace();
            System.out.println("ERROR DURANTE LA EXPORACION DE INDICEINVERTIDO");
        }

    }



    private final Object lock = new Object();
    public void TF(int iDocumento){
        //if(iDocumento%1000 == 0 && iDocumento > 0){
            //System.out.println("Documentos procesados: "+(iDocumento+1)+"/"+nDocumentos);
        //}
        
        List<String> terminosDocumento = obtenerTerminos(documentos[iDocumento]);
        Set<String> setTerminosDocumento = new HashSet<String>(terminosDocumento);
        synchronized(lock){
            terminos.addAll(setTerminosDocumento);
        }
        

        for(String termino : setTerminosDocumento){
            int nApariciones = Collections.frequency(terminosDocumento,termino);
            float valorTF = 1+(float)(Math.log(nApariciones)/(float)Math.log(2));
            synchronized(lock){
                if(!indiceInvertido.containsKey(termino)){
                    Object[] tupla = {0.0f, new HashMap<String, Float>()};
                    indiceInvertido.put(termino, tupla);
                    apariciones.put(termino, 0);
                }

                HashMap<String, Float> hashmap = (HashMap<String, Float>)indiceInvertido.get(termino)[1];
                hashmap.put(documentos[iDocumento], 1+(float)(Math.log(nApariciones)/(float)Math.log(2)));
                apariciones.put(termino, apariciones.get(termino)+1);
            }
        }
    }



    public static void main(String args[]){

        //Obtener lista de documentos
        cargarPalabrasVacias();
        File directorioDocumentos = new File(RUTA_DOCUMENTOS);
        documentos = directorioDocumentos.list();
        int nDocumentos = documentos.length;


        int numeroThreadsEnPool = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(numeroThreadsEnPool);
        indexador miClase = new indexador();


        //CALCULAR TF
        long start = System.currentTimeMillis();
        for(int iDocumento = 0; iDocumento<nDocumentos; iDocumento++){
            final int iDoc = iDocumento;
            executorService.execute(() -> miClase.TF(iDoc));
        }
        executorService.shutdown();
        try {
            // Esperar a que todos los threads del pool terminen
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("Indexacion terminada en: " + timeElapsed + "ms");
        System.out.println(terminos.size() + " terminos");
        

        //CALCULAR IDF
        int nTerminos = terminos.size();

        for(String termino : terminos){
            float idf = (float)(Math.log((nDocumentos/(double)apariciones.get(termino)))/Math.log(2));
            indiceInvertido.get(termino)[0] = idf;
            HashMap<String, Float> hashmap = (HashMap<String, Float>)indiceInvertido.get(termino)[1];
            for(String documento : hashmap.keySet())
                hashmap.put(documento, hashmap.get(documento) * idf);
        }
        

        //EXPORTAR
        start = System.currentTimeMillis();
        exportarLongDocumento(indiceInvertido);
        exportarIndiceInvertido(indiceInvertido);
        finish = System.currentTimeMillis();
        timeElapsed = finish - start;
        System.out.println("Exportacion terminada en: " + timeElapsed + "ms");

        /*
        //MOSTRAR TFIDF
        for(String termino : terminos){
            System.out.print(termino+": ");
            HashMap<String, Float> hashmap = (HashMap<String, Float>)indiceInvertido.get(termino)[1];
            for(String documento : hashmap.keySet()){
                System.out.print(hashmap.get(documento)+" ");
            }
            System.out.println("");
        }*/
    }
}