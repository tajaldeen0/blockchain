/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blockchain;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class Header{
    public String previousHash;
    public Timestamp timeStamp;
    public int difficulty;
    public int nonce = 0;
    
    public Header( String previousHash, Timestamp timeStamp,int difficulty) {
        this.previousHash = previousHash;
        this.timeStamp = timeStamp;
        this.difficulty = difficulty;
    } 
    
    public String toString(){
        return previousHash + "&" + timeStamp + "&" + difficulty + "&" + nonce;
    }
}

class Block{
    public int index;
    public Header header;
    public List<String> transactions;
    public String hash;
    
    
    public Block(){}
    
    public Block(int index, Header header, List<String> transactions) {
        this.index = index;
        this.header = header;
        this.transactions = new ArrayList<>(transactions);
        this.hash = getHash();
    }
    
    
    public String getData(){
        String transactionsSegmants = "";
        if(!transactions.isEmpty())
            transactionsSegmants = transactions.toString();
        return index + "|" + header.toString() + "|" + hash + "|" + transactionsSegmants;
    }

    public String getHash(){
        MessageDigest digest = null;
        byte[] bytes = null;
        
        try {
            digest = MessageDigest.getInstance("SHA-256");
            bytes = digest.digest(header.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder(2 * bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xff & bytes[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception ex) {
            System.out.println("error");;
        }
        return "";
    }
}

class BlockChain{
    private List<Block> chain = new ArrayList<>();
    private List<String> unconfirmedTransaction = new ArrayList<>();

    public BlockChain() {
        String path = "src\\output.txt";
        try {
            BufferedReader bufReader = new BufferedReader(new FileReader(path));
            String line = bufReader.readLine(); 
            while (line != null) { 
                Block b = new Block();
                String [] oneLine = line.split("\\|");
                b.index = Integer.parseInt(oneLine[0]);
                String [] headerSegmants = oneLine[1].split("&");
                b.header = new Header(headerSegmants[0],Timestamp.valueOf(headerSegmants[1]),Integer.parseInt(headerSegmants[2]));
                b.hash = oneLine[2];
                String [] trans = oneLine[3].substring(1, oneLine[3].length()-1).split(",");
                b.transactions = new ArrayList<>();
                for(String s : trans){
                    b.transactions.add(s);
                }
                chain.add(b);
                line = bufReader.readLine(); 
            }
            bufReader.close();


//        createGenesisBlock();
        } catch (Exception ex) {
            Logger.getLogger(BlockChain.class.getName()).log(Level.SEVERE, null, ex);
        }


    }
    
    public void createGenesisBlock(){
        addNewTransaction("50BTC -> taj");
        mine();
    }
    
    public Block getLastBlock(){
        return chain.get(chain.size()-1);
    }
    
    public Boolean addBlock(Block block,String proof){
        String prevHash;
        if(chain.isEmpty())
            prevHash = "0";
        else
            prevHash = getLastBlock().hash;
        
        if(!prevHash.equals(block.header.previousHash))
            return false;
        
        if(!isValidProof(block,proof))
            return false;
        
        block.hash = proof;
        chain.add(block);
        saveBlockChain();
        return true;
        
    }
    
    public void saveBlockChain() {
        try { 
            //  src//info.text  C:\\Users\\taj\\Desktop\\جامعة -فصل الثاني 2022\\بلوك تشين\\ass2\\info.txt
            String path = "src\\output.txt";
            
            FileWriter writer = new FileWriter(path);
            for(Block str: chain) {
                writer.write(str.getData() + System.lineSeparator());
            }
            writer.close();
            
        } catch (IOException ex) {
            Logger.getLogger(BlockChain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Boolean isValidProof(Block block,String blockHash){
        String prefixString = new String(new char[block.header.difficulty]).replace('\0', '0');
        return (blockHash.startsWith(prefixString) && blockHash.equals(block.getHash()));
    }
    
    
    public String ProofOfWork(Block block){
        block.header.nonce = 0;
        String computedHash = block.getHash();
        String prefixString = new String(new char[block.header.difficulty]).replace('\0', '0');
        while(!computedHash.startsWith(prefixString)){
            block.header.nonce +=1;
            computedHash = block.getHash();
            System.out.println(computedHash);
        }
        return computedHash;
    }
    
    public void addNewTransaction(String transaction){
        unconfirmedTransaction.add(transaction);
    }
    
    public Integer mine(){
        if(unconfirmedTransaction.isEmpty())
            return null;
        Date date = null;Block newBlock;
        if(chain.size() ==0){
            date = new Date();
            Header newHeader = new Header( "0", new Timestamp(date.getTime()),4);
            newBlock = new Block(0, newHeader, unconfirmedTransaction);
        }else{
            date = new Date();
            Block lastBlock = getLastBlock();
            Header newHeader = new Header( lastBlock.hash, new Timestamp(date.getTime()),4);
            newBlock = new Block(lastBlock.index + 1, newHeader, unconfirmedTransaction);
        }
        String proof = ProofOfWork(newBlock);
        addBlock(newBlock, proof);
        unconfirmedTransaction.clear();
        
        return newBlock.index;
    }
}


public class Main {
    public static void main(String[] args) {
        BlockChain blockChain = new BlockChain();
        blockChain.addNewTransaction("send 30btc to taj");
        blockChain.addNewTransaction("take 6 btc from abd");
        blockChain.mine();
    }
}
