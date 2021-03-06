package br.ufrn.imd.controller;

import br.ufrn.imd.controller.interfaces.PreProcessing;
import br.ufrn.imd.controller.interfaces.SimilarityAnalysis;
import br.ufrn.imd.model.News;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import br.ufrn.imd.dao.NewsDao;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.max;
import static java.lang.Integer.parseInt;

/*
*
* classe abstrata responsável por manipular os dados
*
* pre-processar os dados (tratamento)
* executar os algoritmos de comparação (análise)
* implementa as interfaces SimilarityAnalysis e PreProcessing
*
* */
public abstract class ManipuleData implements SimilarityAnalysis, PreProcessing {

    protected NewsDao dao;

    //implementação do algoritmo Distância Levenshtein
    @Override
    public double levDistance(String txt1, String txt2) {
        int greaterStr=Integer.max(txt1.length(),txt2.length());

        LevenshteinDistance levenshteinDistance=new LevenshteinDistance();
        int distance=levenshteinDistance.apply(txt1,txt2);

        double normalizedIndex=(greaterStr-distance)/greaterStr;

        return normalizedIndex;
    }

    //implementação do algoritmo Jaro-Winkler
    @Override
    public double jaroWinklerSimilarity(String txt1, String txt2) {

        JaroWinklerSimilarity jaroWinklerSimilarity=new JaroWinklerSimilarity();

        return jaroWinklerSimilarity.apply(txt1,txt2);
    }

    //método para preprocessamento da string
    @Override
    public String cleanString(String originalText) {

       //System.out.println("TEXTO ORIGINAL:" + originalText);

       //normalizando textooriginal para manter um padrão unicode
       String text = Normalizer.normalize(originalText, Normalizer.Form.NFD);

        //retirando caracteres
        String text_1 = (text.replaceAll("[.,!?*()+:-]"," ")).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        //colocando em minusculo
        String text_2 = text_1.toLowerCase();

        //transformando string em array para facilitar as operações
        String[] strArr= text_2.toLowerCase().split(" ");
        List<String> listStrings = new ArrayList<String>(Arrays.asList(strArr));

        //removendo palavras com tamanho menor ou igual a 3
       listStrings.removeIf(str-> str.length()<=3);

        //removendo palavras repetidas
       List<String> finalList = listStrings.stream().distinct().collect(Collectors.toList());

       //ordenando palavras
        Collections.sort(finalList);


       //System.out.println("TEXTO MODIFICADO: " + String.join(" ", finalList));

        return String.join(" ", finalList);
    }


        //método para transformar um dado no tipo News
       @Override
       public News buildDataToNews(List<String> data){
            News fakenews = new News();
            fakenews.setId(parseInt(data.get(0)));
            fakenews.setText_original(data.get(1));
            fakenews.setLink(data.get(2));
            String[] data1 = fakenews.getText_original().split(" ");
            fakenews.setText_format(cleanString(fakenews.getText_original()));
            fakenews.setTimestamp(convertTimestamp(data.get(3)));
            return fakenews;
        }

        //método para converter timestam no formato de data e tratar a exceção
        private LocalDateTime convertTimestamp(String timestamp) {

        LocalDateTime timestampConvert = null;

        try{
                timestampConvert = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }catch (Exception e){
               System.out.println("ocorreu um erro ao gerar o timestamp: " + e);
            }
            return timestampConvert;
    }


        //método para testar a similariedade das string com o dataset
        @Override
        public double testSimilarity(String text) {

            List<News>  fakenews = this.dao.findAllNews();
            News dataUserAnalise = this.dao.getDataUser();
            double levdistance = 0;
            double jaroWinklerSimilarity = 0;
            double levdistanceMedian = 0;
            double jaroWinklerSimilarityMedian = 0;

            for (News news: fakenews)
            {
                //roda o algoritmo para todas as mensagens do dataset
                levdistance += levDistance(news.getText_format(), dataUserAnalise.getText_format());

                //roda o algoritmo para todas as mensagens do dataset
                jaroWinklerSimilarity += jaroWinklerSimilarity(news.getText_format(), dataUserAnalise.getText_format());
            }

            //faz a media pros valores adquiridos nos testes
            levdistanceMedian = levdistance/fakenews.size();

            //faz a media pros valores adquiridos nos testes
            jaroWinklerSimilarityMedian = jaroWinklerSimilarity/fakenews.size();


            //retorna o melhor resultado entre os dois
            return  Double.max(levdistanceMedian, jaroWinklerSimilarityMedian);
        }

        //metodo abstrato que deve ser implementado pelas classes filhas de ManipuleData
        public abstract void saveData(News news);
}
