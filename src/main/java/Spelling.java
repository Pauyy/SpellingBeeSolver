import com.microsoft.playwright.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Spelling {

    private static String mainLetter;
    private static String restLetters;
    private static final String FILENAME = "wordList.txt";

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(100));
            Page page = browser.newPage();
            page.navigate("https://www.nytimes.com/puzzles/spelling-bee");

            dismissCookies(page);
            clickPlay(page);
            getLetters(page);
            List<String> allCorrectWords = findCorrectWords();
            System.out.println(allCorrectWords);
            typeAllWords(allCorrectWords, page);


            page.waitForTimeout(5000);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void dismissCookies(Page page){
        page.click("button#pz-gdpr-btn-reject");
    }

    private static void clickPlay(Page page){
        page.click("button.pz-moment__button");
    }

    private static void getLetters(Page page){
        //Selects all hives and retrieve the letters
        ElementHandle hive = page.querySelector("div.hive");
        List<ElementHandle> cellLetters = hive.querySelectorAll("text.cell-letter");
        //The .toList() creates an unmodifiable list
        List<String> eachLetter = cellLetters.stream().map(ElementHandle::textContent).collect(Collectors.toList());//First Element is the main letter

        mainLetter = eachLetter.get(0).toLowerCase();
        eachLetter.remove(0); //mainLetter is not needed anymore
        StringBuilder remainingLetters = new StringBuilder();
        for(String cha : eachLetter){
            remainingLetters.append(cha);
        }
        restLetters = remainingLetters.toString();
    }

    private static List<String> findCorrectWords() throws FileNotFoundException {
        if(mainLetter == null || restLetters == null)
            throw new IllegalStateException("findCorrectWords called before getLetters");

        //puts allWords that are to check in the scanner
        Scanner allWords = new Scanner(new File(FILENAME));

        //regex Pattern to find words with the allowed letters
        Pattern p = Pattern.compile("[" + restLetters + mainLetter +"]*");

        //list where all correct Words will be stored in
        List<String> correctWords = new ArrayList<>();


        //check every available word
        while(allWords.hasNextLine()){
            //fetch next word
            String aktWord = allWords.nextLine();

            //if word is for somewhat reason empty skip it
            if(aktWord.isBlank())
                continue;
            //if the word is shorter than 4 letters it does not count
            if(aktWord.length() < 4)
                continue;

            //check if the main Letter is included
            if(aktWord.contains(mainLetter)) {
                Matcher m = p.matcher(aktWord);
                //check if the rest of the letters are included
                if(m.matches())
                    correctWords.add(aktWord);
            }
        }

        return correctWords;
    }

    private static void typeAllWords(List<String> wordList, Page page){

        for(String aktWord : wordList){

            for(char c : aktWord.toCharArray()){
                page.keyboard().press(Character.toString(c));
            }
            page.keyboard().press("Enter");
        }
    }

}
