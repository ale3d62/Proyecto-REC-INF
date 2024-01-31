import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Double;
import java.lang.Integer;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.tartarus.snowball.ext.englishStemmer;
import java.lang.Math;



public class Buscador {


	//PARAMETROS
	//------------------------------------------------------------------------------------------------
	public static String ruta_documentos = "corpus"; // Ruta donde se encuentran los documentos de docs
	// Ruta donde se encuentra el fichero con las palabras vacías
	// hay palabras en inglés solamente ya que es el idioma en el que están escritos
	// los documentos, la consulta deberá hacerse también en inglés o no devolverá
	// ningún resultado
	public static String ruta_fichero_pv = "palabras_vacias.txt";
	public static String ruta_lon_doc = "longitud_documentos.json"; // Ruta del fichero donde se encuentran los documentos junto con su longitud
	public static String ruta_indice = "indice_invertido.json";     // Ruta del fichero donde se encuentra el índice invertido
	public static Integer n = 10; // Numero maximo de documentos a devolver por consulta
	//------------------------------------------------------------------------------------------------




    public static void main(String[] args) {
        try {
        	// Almacenamos en un map el índice invertido
            HashMap<String, Object[]> indice_invertido = LeerIndiceInvertido(ruta_indice);   
            // Almacenamos en un map los documentos junto con su longitud
            HashMap<String, Double> documentos_longitud = LeerLongitudDocumentos(ruta_lon_doc);
            // Le pedimos al usuario que introduzca una consulta
            Scanner scanner = new Scanner(System.in);
            System.out.println("Introduzca la consulta:");
            String consulta = scanner.nextLine();
            // Comprobamos si la consulta se trata de una frase
            boolean esFrase = false;
            if (consulta.charAt(0) == '\"' && consulta.charAt(consulta.length()-1) == '\"') {
            	esFrase = true;
            	// Como ya sabemos que es una frase quitamos los caracteres " que la delimita
            	consulta = consulta.substring(1, consulta.length()-1);
            }
            // En caso de que no sea una frase
            if (!esFrase) {
                String consultaProcesada = new String("");
            	// Procesamos la consulta a nivel de caracter
            	consultaProcesada = preprocesarCaracteres(consulta);
            	// Obtenemos la lista de términos de la consulta (almacenado en un HashSet ya que no se tienen
            	// en cuenta los términos duplicados)
            	HashSet<String> terminosConsulta = obtenerTerminos(consultaProcesada);
            	// Procesamos los términos para eliminar las palabras vacías, hacer el stemming
            	// y eliminar los términos cuya longitud esté por debajo de una longitud determinada
            	terminosConsulta = preprocesarTerminos(terminosConsulta);
            	// Le pedimos al usuario el tipo de consulta (AND o OR)
            	boolean tipo_correcto = true;
            	String tipo;
            	scanner.reset();
            	do {
            		if (!tipo_correcto) {
            			System.out.println("Elección de tipo errónea, vuelva a introducirlo");
            		}
            		System.out.print("Introduce el tipo de consulta [y] para AND [o] para OR: ");
            		tipo = scanner.nextLine();
            		if (tipo.equals("y") | tipo.equals("o"))
            			tipo_correcto = true;
            		else
            			tipo_correcto = false;
            	} while (!tipo_correcto);
            	scanner.close();
            	// Obtenemos los documentos en los que aparecen al menos uno de los términos
            	// de la consulta, para cada documento se almacenará también la similitud que tenga
            	// con la consulta
            	HashMap<String, Double> documentos_similitud = obtenerDocumentosSimilitud(terminosConsulta, indice_invertido, documentos_longitud);
            	// Si el tipo de consulta seleccionada es OR entonces no hacemos nada más ya que en
            	// documentos_similitud estarán todos los documentos en los que aparezca al menos
            	// un término de la consulta, sin embargo si el tipo seleccionado es AND tendremos
            	// que extraer de documentos_similitud todos aquellos documentos en los que no aparezcan
            	// al menos uno de los términos de la consulta, ya que el usuario quiere que le devolvamos
            	// los documentos en los que aparecen todos los términos de la consulta, para ello
            	// calculamos la intersección de las listas de documentos de cada término de la
            	// consulta, y dejamos en documentos_similitud solamente aquellos que están en la 
            	// intersección
            	if (tipo.equals("y")) {
            		// Calculamos la intersección
            		HashSet<String> is = interseccion(terminosConsulta, indice_invertido);
            		// Dejamos en documentos_similitud solamente aquellos documentos que aparezcan
            		// en la intersección
            		documentos_similitud = aplicarAND(documentos_similitud, is);
            	}
            	// Ordenamos los documentos en orden decreciente de similitud con la consulta
            	// Devuelve los n primeros documentos de esa lista de documentos ordenada en caso
            	// de que n <= numero_documentos o todos los documentos en caso de que n > numero_documentos
            	List<String> documentos_rankeados = ranking(documentos_similitud, n);
            	// Imprimimos los documentos
				System.out.println("Resultado de la consulta: ");
            	System.out.println(documentos_rankeados.toString());
            }
            // En caso de que sí sea una frase
            else {
            	// Obtenemos los términos de la frase sin procesar
            	String[] terminos = consulta.split(" ");
            	// Obtenemos los términos de la frase procesados
            	String consultaProcesada = new String("");
            	consultaProcesada = preprocesarCaracteres(consulta);
            	HashSet<String> terminosConsulta = obtenerTerminos(consultaProcesada);
            	terminosConsulta = preprocesarTerminos(terminosConsulta);
            	// Para que un documento contenga una frase ha de tener todos los términos que
            	// aparecen en esa frase, por tanto calculamos la intersección de las listas
            	// de documentos de los términos de la frase
            	HashSet<String> is = interseccion(terminosConsulta, indice_invertido);
            	// Ahora cargamos el contenido de esos documentos
            	HashMap<String, String> docs_contenido = cargar_contenido(is);
            	// Finalmente almacenamos únicamente los documentos en los que haya al menos
            	// una aparición de la frase y para cada uno de ellos una lista de intervalos
            	// que indican posición inicial y final de cada aparición de la frase en el documento
            	HashMap<String, ArrayList<Integer[]>> docs_frase = devolver_intervalos(terminos, docs_contenido);
            	// Imprimimos el resultado
            	ArrayList<Integer[]> intervalos = new ArrayList<Integer[]>();
				System.out.println("Resultado de la consulta: ");
            	System.out.println("{");
            	for (HashMap.Entry<String, ArrayList<Integer[]>> doc_frase : docs_frase.entrySet()) {
            		System.out.print("\t" + doc_frase.getKey() + " : [");
            		intervalos = doc_frase.getValue();
            		for (Integer[] intervalo : intervalos)
            			System.out.print(" [" + intervalo[0] + ", " + intervalo[1] + "] ");
            		System.out.println("]");
            	}
            	System.out.println("}");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Método al cual le pasamos la ruta del fichero donde almacenamos el índice invertido y lee
    // y devuelve el índice invertido
    private static HashMap<String, Object[]> LeerIndiceInvertido(String ruta_indice) throws IOException {
    	Gson gson = new Gson();
    	return gson.fromJson(new FileReader(ruta_indice), new TypeToken<HashMap<String, Object[]>>() {}.getType());
    }
    // Método al cual le pasamos la ruta del fichero donde almacenamos la longitud de cada uno de
    // los documentos y lee y devuelve los documentos junto con su longitud
    private static HashMap<String, Double> LeerLongitudDocumentos(String ruta_lon_doc) throws IOException {
    	Gson gson = new Gson();
    	return gson.fromJson(new FileReader(ruta_lon_doc), new TypeToken<HashMap<String, Double>>() {}.getType());
    }
    // Método para procesar la consulta a nivel de caracter
    private static String preprocesarCaracteres(String consulta) {
    	String consP = new String("");
    	// Convertir letras mayúsculas a minúsculas
    	consP = consulta.toLowerCase();
    	// Eliminar todos los signos de puntuación excepto los guiones, letras con tildes y la ñ
    	consP = consP.replaceAll("([^-áéíóúñ\\w])|_", " ");
    	// Elimina todos los números que no forman parte de ninguna palabra
    	consP = consP.replaceAll("\\b[0-9]+\\b", " ");
    	//Elimina todos los guiones que no unan 2 palabras
    	consP = consP.replaceAll("[-]+ | [-]+", " ");
    	//Sustituir todas las vocales con tilde por la misma vocal sin tilde
    	consP = consP.replace('á', 'a');
    	consP = consP.replace('é', 'e');
    	consP = consP.replace('í', 'i');
    	consP = consP.replace('ó', 'o');
    	consP = consP.replace('ú', 'u');
    	// Elimina los espacios duplicados
    	consP = consP.replaceAll("[ ]+", " ");
    	return consP;
    }
    // Método con el que obtenemos los términos de la consulta
    private static HashSet<String> obtenerTerminos(String consultaProcesada) {
    	HashSet<String> terminosConsulta = new HashSet<String>();
    	String[] terminos = consultaProcesada.split(" ");
    	for (String termino : terminos) {
    		terminosConsulta.add(termino);
    	}
    	return terminosConsulta;
    }
    // Método para procesar los términos de la consulta
    private static HashSet<String> preprocesarTerminos(HashSet<String> terminos) {
    	// Almacenaremos las palabras vacías en esta estructura de datos
    	HashSet<String> palabras_vacias = new HashSet<String>();

    	// Leemos el fichero y almacenamos las palabras vacías en palabras_vacias
    	try (BufferedReader br = new BufferedReader(new FileReader(ruta_fichero_pv))) {
            String linea;
            String palabra;
            while ((linea = br.readLine()) != null) {
                // Eliminar caracteres espaciadores (\n principalmente) y agregar la palabra a palabras_vacias
                palabra = linea.trim();
                palabras_vacias.add(palabra);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    	// Quitamos las palabras vacías de la lista de términos
    	terminos.removeAll(palabras_vacias);
    	// Hacemos el stemming
    	// Declaramos el lematizador
    	englishStemmer lematizador = new englishStemmer();
    	// Almacenamos en terminosProcesados los términos lematizados
    	HashSet<String> terminosProcesados = new HashSet<String>();
    	// En este bucle se lematizan los términos y se almacenan en terminosProcesados
    	for (String termino : terminos) {
    		// La lematización se aplica a las palabras que no contengan guión
            if (!termino.contains("-")) {
                lematizador.setCurrent(termino);
                lematizador.stem();
                terminosProcesados.add(lematizador.getCurrent());
            } else {
                terminosProcesados.add(termino); // Conservar términos que contienen "-"
            }
        }
    	// Finalmente eliminamos aquellos términos que estén formados por un número de caracteres
    	// menor que longitud_minima
    	// Declaramos longitud_minima
    	Integer longitud_minima = 3;
    	List<String> terminos_pequennos = new ArrayList<String>();
    	// En este bucle almacenamos en una lista todos los términos con longitud < longitud_minima
    	for (String termino : terminosProcesados) {
    		if (termino.length() < longitud_minima) {
    			terminos_pequennos.add(termino);
    		}
    	}
    	// Aquí eliminamos los términos con longitud < longitud_minima de terminosProcesados
    	terminosProcesados.removeAll(terminos_pequennos);
    	return terminosProcesados;
    }
    // Método con el que obtenemos los documentos y la similitud con la consulta
    private static HashMap<String, Double> obtenerDocumentosSimilitud(HashSet<String> terminosConsulta, 
    	HashMap<String, Object[]> indice_invertido, HashMap<String, Double> documentos_longitud) {
    	
    	// Aquí almacenaremos los documentos junto con su similitud con la consulta
    	HashMap<String, Double> documentos_similitud = new HashMap<String, Double>();
    	// Para cada término de la consulta almacenamos aquí tanto su IDF como el HashMap en el 
    	//que se guardan tanto los documentos en los que aparece el término como el TF-IDF del término
    	// en cada uno de esos documentos
    	Object[] IDF_y_documentos;
    	// El IDF anterior mencionado
    	Double IDF;
    	// El HashMap anterior mencionado
    	HashMap<String, Double> documentos_peso;
    	// Lo necesitaremos para obtener el HashMap anterior mencionado
    	LinkedTreeMap<String, Double> ltm;
    	// Almacenaremos el módulo del vector de términos de la consulta
    	Double moduloConsulta = 0.0;
    	for (String termino : terminosConsulta) {
    		IDF_y_documentos = indice_invertido.get(termino);
    		if (IDF_y_documentos != null) {
    			IDF = (Double)IDF_y_documentos[0];
    			moduloConsulta = moduloConsulta + IDF*IDF;
    			ltm = (LinkedTreeMap<String, Double>)IDF_y_documentos[1];
    			documentos_peso = new HashMap<String, Double>(ltm);
    			for (HashMap.Entry<String, Double> documento_peso : documentos_peso.entrySet()) {
    				if (!documentos_similitud.containsKey(documento_peso.getKey()))
    					documentos_similitud.put(documento_peso.getKey(), IDF*documento_peso.getValue());
    				else
    					documentos_similitud.put(documento_peso.getKey(), documentos_similitud.get(documento_peso.getKey()) + IDF*documento_peso.getValue());
    			}
    		}
    	}
    	// Al finalizar el bucle anterior tendremos en moduloConsulta el sumatorio de los 
    	// cuadrados de los pesos de la consulta y en documentos_similitud tendremos todos los
    	// documentos en los que aparezca al menos un término de la consulta junto con el
    	// producto escalar de cada uno de esos documentos con la consulta
    	
    	// Así que finalmente para calcular el módulo de la consulta le asignamos a moduloConsulta
    	// la raíz cuadrada de moduloConsulta
    	moduloConsulta = Math.sqrt(moduloConsulta);
    	// Y para calcular la similitud de cada uno de esos documentos con la consulta dividimos
    	// sus productos escalares entre el producto de los módulos de la consulta y el documento
    	for (HashMap.Entry<String, Double> documento_similitud : documentos_similitud.entrySet())
    		documentos_similitud.put(documento_similitud.getKey(), documento_similitud.getValue()/(moduloConsulta*documentos_longitud.get(documento_similitud.getKey())));
    	return documentos_similitud;
    }
    // Método que recibe los términos de la consulta y el índice invertido y devuelve los documentos
    // en los que aparecen todos los términos de la consulta, es decir, devuelve la intersección
    // de las listas de documentos de cada uno de los términos de la consulta
    private static HashSet<String> interseccion(HashSet<String> terminosConsulta, HashMap<String, Object[]> indice_invertido) {
    	HashSet<String> is = new HashSet<String>();
    	List<String> lista_documentos;
    	HashMap<String, Double> documentos_peso;
    	LinkedTreeMap<String, Double> ltm;
    	Object[] IDF_y_documentos;
    	boolean primera_lista = true;
    	for (String termino : terminosConsulta) {
    		IDF_y_documentos = indice_invertido.get(termino);
    		if (IDF_y_documentos != null) {
    			ltm = (LinkedTreeMap<String, Double>)IDF_y_documentos[1];
    			documentos_peso = new HashMap<String, Double>(ltm);
    			lista_documentos = new ArrayList<String>(documentos_peso.keySet());
    			if (primera_lista) {
    				is = new HashSet<String>(lista_documentos);
    				primera_lista = false;
    			}
    			else
    				is.retainAll(lista_documentos);
    		}
    		else {
    			is.clear();
    			break;
    		}
    	}
    	return is;
    }
    // Método para dejar en docs_sim solamente aquellos documentos que aparezcan en la intersección
    private static HashMap<String, Double> aplicarAND(HashMap<String, Double> docs_sim, HashSet<String> is) {
    	HashMap<String, Double> docs_sim_is = new HashMap<String, Double>();
    	for (HashMap.Entry<String, Double> doc_sim : docs_sim.entrySet())
    		if (is.contains(doc_sim.getKey()))
    			docs_sim_is.put(doc_sim.getKey(), doc_sim.getValue());
    	return docs_sim_is;
    }
    // Método que devuelve los documentos ordenados en función de la similitud con la consulta
    private static List<String> ranking(HashMap<String, Double> docs_sim, Integer n) {
    	List<HashMap.Entry<String, Double>> lista_docs_sim = new ArrayList<>(docs_sim.entrySet());
    	Collections.sort(lista_docs_sim, Collections.reverseOrder(HashMap.Entry.comparingByValue()));
    	List<String> documentos_rankeados = new ArrayList<String>();
    	int i = 0;
    	for (HashMap.Entry<String, Double> doc_sim : lista_docs_sim) {
    		if (i == n)
    			break;
    		documentos_rankeados.add(doc_sim.getKey());
    		i++;
    	}
    	return documentos_rankeados;
    }
    // Método para escribir el contenido de los documentos pasados como parámetro en el HashMap
    // que devuelve
    private static HashMap<String, String> cargar_contenido(HashSet<String> docs) {
    	// Estructura de datos donde almacenaremos el contenido de los documentos
    	HashMap<String, String> docs_contenido = new HashMap<String, String>();
    	// Ruta final con el nombre del documento
    	String ruta_final = new String(ruta_documentos);
    	// Iremos almacenando el contenido de cada documento aquí
    	String contenido = new String("");
    	// Bucle en el que recorreremos cada documento y cargando su contenido en docs_contenido
    	for (String doc : docs) {
    		ruta_final = ruta_final + "\\" + doc;
    		// Leemos el contenido del documento y lo almacenamos en docs_contenido
    		try (BufferedReader br = new BufferedReader(new FileReader(ruta_final))) {
                String linea;
                while ((linea = br.readLine()) != null)
                	contenido = contenido + linea + " ";
                docs_contenido.put(doc, contenido);
                ruta_final = ruta_documentos;
                contenido = "";
            } catch (IOException e) {
                e.printStackTrace();
            }
    	}
    	return docs_contenido;
    }
    // Método que dados los términos de una frase, sin eliminar términos repetidos y en el mismo
    // orden en el que aparecen en la frase y dado un HashMap con los documentos en los que aparecen
    // todos los términos de la frase junto con su contenido, devuelve un HashMap en el que aparecen
    // todos los documentos en los que haya al menos una aparición de la frase y para cada uno de 
    // ellos una lista de intervalos que indican la posición inicial y final de cada aparición 
    // de la frase en el documento
    private static HashMap<String, ArrayList<Integer[]>> devolver_intervalos(String[] terminos, HashMap<String, String> docs_contenido) {
    	// Almacenaremos en esta estructura de datos los documentos junto con los intervalos
    	HashMap<String, ArrayList<Integer[]>> docs_intervalos = new HashMap<String, ArrayList<Integer[]>>();
    	// Nombre del documento
    	String nombre_doc = new String("");
    	// Contenido del documento
    	String contenido = new String("");
    	// Palabras del contenido
    	String[] palabras_contenido;
    	// Intervalo de posiciones
    	Integer[] intervalo = new Integer[2];
    	// Recorremos los documentos
    	for (HashMap.Entry<String, String> doc_contenido : docs_contenido.entrySet()) {
    		// Nombre de documento
    		nombre_doc = doc_contenido.getKey();
    		// Contenido del documento
    		contenido = doc_contenido.getValue();
    		// Palabras del contenido
    		palabras_contenido = contenido.split(" ");
    		// Cada intervalo de posiciones del documento en los que aparezca la frase lo almacenamos
    		// aquí
    		intervalo[0] = -1;
    		// En este bucle hallamos todos esos intervalos y los vamos almacenando
    		while (intervalo[0] != -2) {
    			// En cada iteración hallamos un intervalo diferente en el que aparece la frase en
    			// el documento, esta función sirve para hallar uno de esos intervalos
    			intervalo = siguienteFrase(Arrays.asList(terminos), intervalo[0], Arrays.asList(palabras_contenido));
    			// Finalmente si ha encontrado otra aparición de la frase almacenamos el intervalo
    			if (intervalo[0] != -2) {
    				if (!docs_intervalos.containsKey(nombre_doc))
    					docs_intervalos.put(nombre_doc, new ArrayList<Integer[]>());
    				docs_intervalos.get(nombre_doc).add(intervalo);
    			}
    		}
    	}
    	return docs_intervalos;
    }
    // Método que devuelve un intervalo utilizado en el método devolver_intervalos
    private static Integer[] siguienteFrase(List<String> terminos, Integer u, List<String> palabras_contenido) {
    	Integer v = u;
    	Integer num_terminos = terminos.size();
    	Integer[] intervalo = new Integer[2];
    	for (Integer i = 1; i <= num_terminos; i++) {
    		v = siguiente(terminos.get(i-1), v, palabras_contenido);
    		if (v == -2)
    			break;
    	}
    	if (v == -2) {
    		intervalo[0] = -2;
    		return intervalo;
    	}
    	u = v;
    	for (Integer i = num_terminos-1; i > 0; i--)
    		u = anterior(terminos.get(i-1), u, palabras_contenido);
    	if (v-u == num_terminos-1) {
    		intervalo[0] = u;
    		intervalo[1] = v;
    		return intervalo;
    	}
    	else
    		return siguienteFrase(terminos, u, palabras_contenido);
    }
    // Método que dado un término, una posición y una lista de palabras devuelve la posición de la 
    // primera aparición del término en la lista desde la posición siguiente a la dada, si el término
    // no aparece desde esa posición devuelve -2
    private static Integer siguiente(String termino, Integer pos, List<String> palabras) {
    	Integer num_palabras = palabras.size();
    	Integer posicion_pa = -2;
    	for (Integer i = pos+1; i < num_palabras; i++)
    		if (palabras.get(i).equals(termino)) {
    			posicion_pa = i;
    			break;
    		}
    	return posicion_pa;
    }
    // Método que dado un término, una posición y una lista de palabras devuelve la posición de la
    // primera aparición del término en la lista desde la posición anterior a la dada recorriendo
    // la lista hacia atrás
    private static Integer anterior(String termino, Integer pos, List<String> palabras) {
    	boolean encontrado = false;
    	Integer posicion_pa = pos-1;
    	while (!encontrado) {
    		if (palabras.get(posicion_pa).equals(termino))
    			encontrado = true;
    		else
    			posicion_pa--;
    	}
    	return posicion_pa;
    }
}
