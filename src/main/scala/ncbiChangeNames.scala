package at.ac.csf.selenium

import java.util.regex.Pattern
import java.util.concurrent.TimeUnit
import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions


class NCBI(loginName: String, password: String){
  val driver = new FirefoxDriver()
  val baseUrl = "https://www.ncbi.nlm.nih.gov/geo"
  driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)


  def login(){
    driver.get(s"$baseUrl/submitter/")
    driver.findElement(By.linkText("login")).click()
    driver.switchTo().frame(0)
    val uname = driver.findElement(By.id("uname"))
    uname.clear()
    uname.sendKeys(loginName)
    val passwd = driver.findElement(By.id("upasswd"))
    passwd.clear()
    passwd.sendKeys(password)
    driver.findElement(By.id("signinBtn")).click()
  }


  def changeTitle(id: String, newTitle: String){
    println(s"renaming: $id $newTitle")
    driver.get(s"${baseUrl}/query/acc.cgi?acc=$id")
    driver.findElement(By.cssSelector("img[alt=\"Update\"]")).click()
    driver.findElement(By.id("skip")).click()
    driver.findElement(By.id("sample_title")).clear()
    driver.findElement(By.id("sample_title")).sendKeys(newTitle)
    driver.findElement(By.id("ack_already_suparch")).click()
    driver.findElement(By.cssSelector("img[alt=\"Next...\"]")).click()
    driver.findElement(By.name("Submit")).click()
  }  


}

class Renamer(geoin: String, renaming: String){
    def readTab(): Map[String,String] = {
        val src = scala.io.Source.fromFile(renaming)
        val o2n = (for(l <- src.getLines.drop(1)) yield {
            val items = l.split("\t")
            (items(0),items(1))
        }).toMap
        src.close
        o2n
    }    
    
    def readFile(): Map[String,String] = {
        val titleR = """!Sample_title = (.*)""".r
        val accR = """!Sample_geo_accession = (\w*)""".r
    
        val src = scala.io.Source.fromFile(geoin)
        val t2a = (for(l <- src.getLines.grouped(3)) yield {
           val l1 = l(0)
           val l2 = l(1)
           println(l1 +" "+l2)
           val titleR(oldtitle) = l1  
           val accR(acc) = l2
           (oldtitle, acc)
        }).toMap
        src.close
        t2a
    }
    
    def getAcc2New(): Seq[(String,String)] = {
        val o2n = readTab()
        val t2a = readFile()
        val a2n = for{
          (old,newt) <- o2n
        } yield (t2a(old), newt)
        a2n.toList     
    }
        
    def rename(){
        val a2n = getAcc2New
        val ncbi = new NCBI("username", "password")
        ncbi.login()      
        for{
           (acc, newt) <- a2n
        }{
           ncbi.changeTitle(acc, newt)
        }    
    }

}


object NCBIChange {

  def main(args: Array[String]){     
      new Renamer("samples.txt", "geo_rename_4c.txt").rename

  }

}

