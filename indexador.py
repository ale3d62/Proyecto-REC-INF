from math import log2
import os
import time

#-------------PARAMETROS----------------
RUTA_DOCUMENTOS = "corpus"
#---------------------------------------


#def preprocesarTexto(texto):



#def preprocesarTerminos(terminos):




#Lee el contenido del documento y devuelve los terminos relevantes
def obtenerTerminos(documento):
    #Leer contenido del archivo
    archivo = open(RUTA_DOCUMENTOS+"/"+documento, 'r')
    texto = archivo.read()
    archivo.close()

    #texto = preprocesarTexto(texto)

    #Dividir en terminos
    terminos = texto.split()
    #terminos = preprocesarTerminos(terminos)

    return terminos




if __name__ == "__main__":

    #Obtener lista de documentos
    documentos = os.listdir(RUTA_DOCUMENTOS)

    nDocumentos = len(documentos)
    
    TF = {}
    IDF = []
    apariciones = {}
    terminos = set()

    start = time.time()

    for iDocumento, documento in enumerate(documentos):
        if(iDocumento%1000 == 0): 
            print("Documentos procesados: "+str(iDocumento+1)+"/"+str(nDocumentos))

        terminosDocumento = obtenerTerminos(documento)
        setTerminosDocumento = set(terminosDocumento)
        terminos.update(setTerminosDocumento) #añadir terminos al set de terminos global

        for termino in setTerminosDocumento:
            #Si el termino es nuevo, inicializamos su fila en la matriz para poder trabajar
            if(not (termino in apariciones)):
                TF[termino] = ([0]*nDocumentos) 
                apariciones[termino] = 0

            #Calculamos TF
            nApariciones = terminosDocumento.count(termino)
            TF[termino][iDocumento] = (1+log2(nApariciones))
            apariciones[termino] += 1


    for termino in terminos:
        IDF.append(log2(nDocumentos/apariciones[termino]))

    nTerminos = len(terminos)
    TFIDF = [[0]*nDocumentos]*nTerminos

    for i, termino in enumerate(terminos):
        for j in range(nDocumentos):
            TFIDF[i][j] = TF[termino][j] * IDF[i]


    end = time.time()
    elapsed_time = end - start
    print('Execution time:', elapsed_time, 'seconds')