package com.github.dapeng.soa.domain;

        import java.util.Optional;

        /**
         * Autogenerated by Dapeng-Code-Generator (1.2.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

        *
        **/
        public class Info{
        
            /**
            *
            **/
            public String name ;
            public String getName(){ return this.name; }
            public void setName(String name){ this.name = name; }

            public String name(){ return this.name; }
            public Info name(String name){ this.name = name; return this; }
          
            /**
            *
            **/
            public String code ;
            public String getCode(){ return this.code; }
            public void setCode(String code){ this.code = code; }

            public String code(){ return this.code; }
            public Info code(String code){ this.code = code; return this; }
          

        public String toString(){
          StringBuilder stringBuilder = new StringBuilder("{");
          stringBuilder.append("\"").append("name").append("\":\"").append(this.name).append("\",");
    stringBuilder.append("\"").append("code").append("\":\"").append(this.code).append("\",");
    
          stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
          stringBuilder.append("}");

          return stringBuilder.toString();
        }
      }
      